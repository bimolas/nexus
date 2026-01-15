package com.example.nexus.view.components;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Tab;
import com.example.nexus.controller.DownloadController;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.SnapshotParameters;
import javafx.beans.value.ChangeListener;
import java.util.function.Consumer;
import java.util.Locale;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;

import com.example.nexus.service.SettingsService;

public class BrowserTab extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(BrowserTab.class);

    private final DIContainer container;
    private final WebView webView;
    private final WebEngine webEngine;
    private final ProgressBar loadingBar;
    private final StackPane contentPane;
    private final ScrollPane scrollPane;
    private final javafx.scene.Group zoomGroup;
    private ContextMenu linkContextMenu;

    private final StringProperty titleProperty = new SimpleStringProperty("New Tab");
    private final StringProperty urlProperty = new SimpleStringProperty("");
    private final StringProperty faviconUrlProperty = new SimpleStringProperty("");
    private final BooleanProperty loadingProperty = new SimpleBooleanProperty(false);
    private final DoubleProperty zoomProperty = new SimpleDoubleProperty(1.0);
    private Tab tabModel;
    private boolean disposed = false;

    private double viewportZoom = 1.0;

    private boolean webPageDarkMode = false;
    private static boolean globalDarkModeEnabled = false;

    private ChangeListener<javafx.geometry.Bounds> viewportBoundsListener;
    private ChangeListener<String> titleChangeListener;
    private ChangeListener<String> locationChangeListener;
    private ChangeListener<Worker.State> stateChangeListener;
    private ChangeListener<Number> progressChangeListener;
    private ChangeListener<Throwable> exceptionChangeListener;

    private final java.util.concurrent.atomic.AtomicBoolean headCheckRunning = new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile long lastHeadCheckAtMs = 0;
    private static final java.util.concurrent.ExecutorService HEAD_EXECUTOR = java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "nexus-head-check");
        t.setDaemon(true);
        return t;
    });
    private volatile long lastFaviconUpdateAtMs = 0;

    public BrowserTab(DIContainer container, String url) {
        this.container = container;

        this.webView = new WebView();
        this.webEngine = webView.getEngine();

        // Set reasonable size constraints to prevent texture allocation issues
        webView.setMinWidth(400);
        webView.setMinHeight(300);
        webView.setMaxWidth(Double.MAX_VALUE);
        webView.setMaxHeight(Double.MAX_VALUE);

        webView.setCache(true);
        webView.setContextMenuEnabled(false);  // Replace default context menu with custom one
        setupContextMenu();

        webEngine.setJavaScriptEnabled(true);


        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15";
        webEngine.setUserAgent(userAgent);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            loadingProperty.set(newState == Worker.State.RUNNING || newState == Worker.State.SCHEDULED);
        });

        webView.zoomProperty().addListener((obs, oldZoom, newZoom) -> {
            zoomProperty.set(newZoom.doubleValue());
        });

        this.loadingBar = new ProgressBar();
        loadingBar.setMaxWidth(Double.MAX_VALUE);
        loadingBar.setPrefHeight(3);
        loadingBar.setVisible(false);
        loadingBar.setStyle("-fx-accent: #4285f4;");

        this.zoomGroup = new javafx.scene.Group(webView);

        this.scrollPane = new ScrollPane(zoomGroup);
        // Disable ScrollPane scrollbars - WebView handles its own scrolling
        // This prevents scrollbar flickering and glitching issues
        scrollPane.setPannable(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: #ffffff; -fx-background: #ffffff;");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // Make ScrollPane non-focusable to prevent interference
        scrollPane.setFocusTraversable(false);

        this.contentPane = new StackPane(scrollPane);
        contentPane.setStyle("-fx-background-color: #ffffff;");

        this.setStyle("-fx-background-color: #ffffff;");

        setTop(loadingBar);
        setCenter(contentPane);

        // Simplified viewport bounds listener - only resize on significant changes
        viewportBoundsListener = (obs, oldBounds, newBounds) -> {
            if (disposed || viewportZoom != 1.0) return;
            if (newBounds == null) return;

            double newWidth = Math.max(400, newBounds.getWidth());
            double newHeight = Math.max(300, newBounds.getHeight());

            // Only update if change is significant (more than 10 pixels)
            double currentWidth = webView.getPrefWidth();
            double currentHeight = webView.getPrefHeight();

            if (Math.abs(currentWidth - newWidth) > 10 ||
                Math.abs(currentHeight - newHeight) > 10) {
                webView.setPrefWidth(newWidth);
                webView.setPrefHeight(newHeight);
            }
        };
        scrollPane.viewportBoundsProperty().addListener(viewportBoundsListener);

        setupWebEngineHandlers();

        webEngine.setCreatePopupHandler(config -> {
            WebView popupView = new WebView();
            popupView.getEngine().setJavaScriptEnabled(true);
            popupView.getEngine().setUserAgent(userAgent);
            return popupView.getEngine();
        });

        webEngine.setOnAlert(event -> logger.info("JavaScript Alert: {}", event.getData()));


        if (url != null && !url.isEmpty()) {
            loadUrl(url);
        }

        logger.info("BrowserTab created with URL: {}", url);
    }

    public BooleanProperty loadingProperty() {
        return loadingProperty;
    }

    public DoubleProperty zoomProperty() {
        return zoomProperty;
    }

    public double getZoom() {
        return zoomProperty.get();
    }
    private void setupWebEngineHandlers() {

        titleChangeListener = (obs, oldTitle, newTitle) -> {
            if (disposed) return;
            Platform.runLater(() -> {
                if (!disposed) {
                    titleProperty.set(newTitle != null && !newTitle.isEmpty() ? newTitle : "New Tab");
                }
            });
        };
        webEngine.titleProperty().addListener(titleChangeListener);

        locationChangeListener = (obs, oldUrl, newUrl) -> {
            if (disposed) return;
            Runnable handler = () -> {
                if (disposed) return;
                 try {
                     String urlStr = newUrl != null ? newUrl : "";

                     if (urlStr != null && !urlStr.isEmpty()) {
                         String lower = urlStr.split("\\?")[0].toLowerCase();
                         // Only auto-download non-image files - let images display normally
                         if (lower.matches(".*\\.(zip|exe|msi|pdf|docx?|xlsx?|rar|7z)$")) {
                             try {
                                 String fileName = suggestFileNameFromUrl(urlStr);
                                 DownloadController dc = container.getOrCreate(DownloadController.class);
                                 dc.requestDownloadFromUI(urlStr, fileName);

                                 try { webEngine.getLoadWorker().cancel(); } catch (Exception ignore) {}
                                 logger.info("Intercepted navigation and started download: {}", urlStr);
                                 return;
                             } catch (Exception ex) {
                                 logger.debug("Failed to start download for intercepted URL: {}", urlStr, ex);
                             }
                         } else if (isDirectImageUrl(urlStr)) {
                             // Skip HEAD check for direct image navigation - let them display
                             logger.debug("Direct image navigation detected, skipping download check: {}", urlStr);
                         } else {

                            // Throttle HEAD checks: they are expensive and can make browsing/video playback stutter.
                            if (!shouldRunHeadCheck(urlStr)) {
                                // skip
                            } else {
                                long now = System.currentTimeMillis();
                                if ((now - lastHeadCheckAtMs) < 1500) {
                                    // too soon
                                } else if (!headCheckRunning.compareAndSet(false, true)) {
                                    // a head check is already running
                                } else {
                                    lastHeadCheckAtMs = now;
                                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                                  HttpURLConnection conn = null;
                                  try {
                                      java.net.URL u = new java.net.URL(urlStr);
                                      conn = (HttpURLConnection) u.openConnection();
                                      conn.setRequestMethod("HEAD");
                                      conn.setInstanceFollowRedirects(true);
                                      conn.setConnectTimeout(8000);
                                      conn.setReadTimeout(8000);
                                      conn.connect();

                                    String disposition = conn.getHeaderField("Content-Disposition");
                                    String contentType = conn.getContentType();
                                    int code = conn.getResponseCode();

                                    boolean looksLikeAttachment = false;
                                    if (disposition != null && disposition.toLowerCase().contains("attachment")) looksLikeAttachment = true;
                                    if (disposition != null && (disposition.toLowerCase().contains("filename=") || disposition.toLowerCase().contains("filename*="))) looksLikeAttachment = true;
                                    if (contentType != null) {
                                        String ct = contentType.toLowerCase();

                                        // Exclude images - let them display in browser
                                        if (!ct.startsWith("image/")) {
                                            if (ct.startsWith("application/") || ct.startsWith("video/") || ct.startsWith("audio/") ) looksLikeAttachment = true;

                                            if (ct.equals("application/octet-stream")) looksLikeAttachment = true;
                                        }
                                    }

                                    boolean nonHtml = contentType == null || !contentType.toLowerCase().contains("text/html");
                                    // Also exclude images from non-HTML check
                                    boolean isImage = contentType != null && contentType.toLowerCase().startsWith("image/");
                                    if ((looksLikeAttachment || (nonHtml && !isImage)) && (code / 100 == 2)) {

                                        String suggested = null;
                                        if (disposition != null) {

                                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("filename\\*?=\\s*(?:\\\"?)(?:UTF-8'')?([^;\\\"]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(disposition);
                                            if (m.find()) {
                                                suggested = m.group(1).trim().replaceAll("[\\\"]", "");
                                                try { suggested = java.net.URLDecoder.decode(suggested, java.nio.charset.StandardCharsets.UTF_8); } catch (Exception ignored) {}
                                            }
                                        }
                                        if (suggested == null || suggested.isEmpty()) suggested = suggestFileNameFromUrl(urlStr);

                                        final String fileName = suggested;

                                        javafx.application.Platform.runLater(() -> {
                                            try {
                                                DownloadController dc = container.getOrCreate(DownloadController.class);
                                                dc.requestDownloadFromUI(urlStr, fileName);
                                                try { webEngine.getLoadWorker().cancel(); } catch (Exception ignore) {}
                                                logger.info("HEAD-detected download and started: {} (suggested={})", urlStr, fileName);
                                            } catch (Exception ex) {
                                                logger.debug("Failed to start HEAD-detected download for {}", urlStr, ex);
                                            }
                                        });
                                    }
                                } catch (Exception e) {

                                    logger.debug("HEAD check failed for {}: {}", urlStr, e.getMessage());
                                } finally {
                                    if (conn != null) conn.disconnect();
                                    headCheckRunning.set(false);
                                }
                            }, HEAD_EXECUTOR);
                                }
                            }
                          }
                      }

                    urlProperty.set(newUrl != null ? newUrl : "");

                    long nowFav = System.currentTimeMillis();
                    if ((nowFav - lastFaviconUpdateAtMs) > 1000) {
                        lastFaviconUpdateAtMs = nowFav;
                        updateFaviconUrl(newUrl);
                    }
                    logger.debug("Location changed to: {}", newUrl);
                 } catch (Exception e) {
                     logger.debug("Error processing location change", e);
                 }
            };
            if (Platform.isFxApplicationThread()) handler.run(); else Platform.runLater(handler);
          };
          webEngine.locationProperty().addListener(locationChangeListener);

        stateChangeListener = (obs, oldState, newState) -> {
            if (disposed) return;
            Platform.runLater(() -> {
                if (disposed) return;
                logger.debug("WebEngine state changed: {} -> {}", oldState, newState);

                if (newState == null) return;

                switch (newState) {
                    case READY -> loadingBar.setVisible(false);
                    case SCHEDULED -> {
                        loadingBar.setVisible(true);
                        loadingBar.setProgress(0);
                    }
                    case RUNNING -> {
                        loadingBar.setVisible(true);
                        loadingBar.setProgress(-1);
                    }
                    case SUCCEEDED -> {
                        if (disposed) return;
                        loadingBar.setVisible(false);
                        loadingBar.setProgress(1.0);

                        extractFaviconFromPage();

                        Platform.runLater(() -> {
                            if (!disposed) installDownloadBridge();
                        });

                        reapplyDarkModeIfEnabled();
                        logger.info("Page loaded successfully: {}", webEngine.getLocation());
                    }
                    case CANCELLED -> {
                        loadingBar.setVisible(false);

                        logger.debug("Page load cancelled");
                    }
                    case FAILED -> {
                        loadingBar.setVisible(false);
                        Throwable ex = webEngine.getLoadWorker().getException();
                        if (ex != null) {
                            logger.error("Page load failed: {}", ex.getMessage());
                            showErrorView("Failed to load page: " + ex.getMessage());
                        }
                    }
                }
            });
        };
        webEngine.getLoadWorker().stateProperty().addListener(stateChangeListener);

        progressChangeListener = (obs, oldProgress, newProgress) -> {
            if (disposed) return;
            Platform.runLater(() -> {
                if (disposed) return;
                double progress = newProgress.doubleValue();
                if (progress >= 0 && progress < 1.0) {
                    loadingBar.setProgress(progress);
                } else if (progress >= 1.0) {
                    loadingBar.setProgress(1.0);
                }
            });
        };
        webEngine.getLoadWorker().progressProperty().addListener(progressChangeListener);

        exceptionChangeListener = (obs, oldEx, newEx) -> {
            if (disposed) return;
            if (newEx != null) {
                logger.error("WebEngine exception: {}", newEx.getMessage(), newEx);
                Platform.runLater(() -> {
                    if (!disposed) loadingBar.setVisible(false);
                });
            }
        };
        webEngine.getLoadWorker().exceptionProperty().addListener(exceptionChangeListener);

        webEngine.setOnError(event -> {
            if (!disposed) {
                logger.error("WebEngine error event: {}", event.getMessage());
            }
        });
    }

    private void updateFaviconUrl(String pageUrl) {
        if (pageUrl == null || pageUrl.isEmpty()) {
            faviconUrlProperty.set("");
            return;
        }

        try {
            URI uri = new URI(pageUrl);
            String host = uri.getHost();
            if (host != null) {

                String faviconUrl = "https://www.google.com/s2/favicons?domain=" + host + "&sz=32";
                faviconUrlProperty.set(faviconUrl);
            }
        } catch (Exception e) {
            logger.debug("Could not parse URL for favicon: {}", pageUrl);
            faviconUrlProperty.set("");
        }
    }

    private void extractFaviconFromPage() {
        try {

            String script = """
                (function() {
                    var link = document.querySelector("link[rel*='icon']");
                    if (link) return link.href;
                    var shortcut = document.querySelector("link[rel='shortcut icon']");
                    if (shortcut) return shortcut.href;
                    return null;
                })()
                """;
            Object result = webEngine.executeScript(script);
            if (result != null && !result.toString().equals("null")) {
                faviconUrlProperty.set(result.toString());
            }
        } catch (Exception e) {

            logger.debug("Could not extract favicon from page", e);
        }
    }

    public WritableImage getPreviewSnapshot() {
        if (disposed || webView == null) return null;
        try {
            double w = Math.max(1, webView.getWidth());
            double h = Math.max(1, webView.getHeight());

            final double MAX_W = 320;
            final double MAX_H = 200;

            double scale = Math.min(1.0, Math.min(MAX_W / w, MAX_H / h));

            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(new Scale(scale, scale));

            int outW = Math.max(1, (int) Math.round(w * scale));
            int outH = Math.max(1, (int) Math.round(h * scale));

            WritableImage out = new WritableImage(outW, outH);
            return webView.snapshot(params, out);
        } catch (Exception e) {
            logger.debug("Could not create page snapshot", e);
            return null;
        }
    }

    /**
     * Backwards-compatible alias used by TabController preview code.
     */
    public WritableImage getCachedPreviewSnapshot() {
        return getPreviewSnapshot();
    }

    private void showErrorView(String message) {
        Label iconLabel = new Label("🌐");
        iconLabel.setStyle("-fx-font-size: 64px;");

        Label errorLabel = new Label(message);
        errorLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-text-alignment: center;");
        errorLabel.setWrapText(true);

        VBox errorBox = new VBox(20);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 40;");
        errorBox.getChildren().addAll(iconLabel, errorLabel);

        contentPane.getChildren().clear();
        contentPane.getChildren().add(errorBox);
    }

    public void loadUrl(String url) {
        if (disposed) return;

        String processedUrl = url;

        if (url != null && !url.isEmpty()) {
            String trimmedUrl = url.trim();

            if (!trimmedUrl.contains(".") && !trimmedUrl.startsWith("http") && !trimmedUrl.startsWith("file")) {

                processedUrl = "https://duckduckgo.com/?q=" + trimmedUrl.replace(" ", "+");
            } else if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://") &&
                !trimmedUrl.startsWith("file://") && !trimmedUrl.startsWith("data:")) {
                processedUrl = "https://" + trimmedUrl;
            } else {
                processedUrl = trimmedUrl;
            }
        }

        final String finalUrl = processedUrl;

        Platform.runLater(() -> {
            loadingBar.setVisible(true);
            loadingBar.setProgress(-1);
        });

        Platform.runLater(() -> {
            try {

                if (webEngine.getLoadWorker().isRunning()) {
                    webEngine.getLoadWorker().cancel();
                }

                webEngine.load(finalUrl);
                urlProperty.set(finalUrl);
                logger.info("Loading URL: {}", finalUrl);
            } catch (Exception e) {
                logger.error("Error loading URL: {}", finalUrl, e);
                loadingBar.setVisible(false);
                showErrorView("Failed to load: " + finalUrl);
            }
        });
    }

    public void goBack() {
        if (disposed) return;
        Platform.runLater(() -> {
            try {
                if (webEngine.getHistory().getCurrentIndex() > 0) {
                    webEngine.getHistory().go(-1);
                }
            } catch (Exception e) {
                logger.error("Error going back", e);
            }
        });
    }

    public void goForward() {
        if (disposed) return;
        Platform.runLater(() -> {
            try {
                if (webEngine.getHistory().getCurrentIndex() < webEngine.getHistory().getEntries().size() - 1) {
                    webEngine.getHistory().go(1);
                }
            } catch (Exception e) {
                logger.error("Error going forward", e);
            }
        });
    }

    public void reload() {
        if (disposed) return;
        Platform.runLater(() -> {
            try {
                webEngine.reload();
            } catch (Exception e) {
                logger.error("Error reloading", e);
            }
        });
    }

    public void stop() {
        if (disposed) return;
        Platform.runLater(() -> {
            try {
                webEngine.getLoadWorker().cancel();
            } catch (Exception e) {
                logger.error("Error stopping", e);
            }
        });
    }

    public void print() {
        if (disposed) return;
        Platform.runLater(() -> {
            try {

                javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
                if (job != null && job.showPrintDialog(webView.getScene().getWindow())) {
                    webEngine.print(job);
                    job.endJob();
                }
            } catch (Exception e) {
                logger.error("Error printing", e);
            }
        });
    }

    public void print(boolean saveAsPdf, boolean printHeaders, boolean printBackground) {

        print();
    }

    public void find(String searchText, boolean forward, boolean matchCase, boolean findNext) {
        if (disposed || searchText == null || searchText.isEmpty()) return;
        Platform.runLater(() -> {
            try {

                String script = String.format(
                    "window.find('%s', %b, %b, true, false, true, false);",
                    searchText.replace("'", "\\'"),
                    matchCase,
                    !forward
                );
                webEngine.executeScript(script);
            } catch (Exception e) {
                logger.error("Error finding text", e);
            }
        });
    }

    public void stopFind() {
        if (disposed) return;
        Platform.runLater(() -> {
            try {
                webEngine.executeScript("window.getSelection().removeAllRanges();");
            } catch (Exception e) {
                logger.debug("Error clearing selection", e);
            }
        });
    }

    public void zoomIn() {
        if (disposed) return;
        Platform.runLater(() -> webView.setZoom(webView.getZoom() * 1.1));
    }

    public void zoomOut() {
        if (disposed) return;
        Platform.runLater(() -> webView.setZoom(webView.getZoom() / 1.1));
    }

    public void resetZoom() {
        if (disposed) return;
        Platform.runLater(() -> webView.setZoom(1.0));
    }

    public void setZoomLevel(double zoomLevel) {
        if (disposed) return;
        Platform.runLater(() -> webView.setZoom(zoomLevel));
    }

    public void setViewportZoom(double zoomLevel) {
        if (disposed) return;
        this.viewportZoom = zoomLevel;

        if (zoomLevel <= 1.0) {
            stopSmoothScrollAnimation();
        }

        Platform.runLater(() -> {

            Scale scale = new Scale(zoomLevel, zoomLevel, 0, 0);
            webView.getTransforms().clear();
            webView.getTransforms().add(scale);

            if (zoomLevel > 1.0) {
                scrollPane.setFitToWidth(false);
                scrollPane.setFitToHeight(false);
                scrollPane.setPannable(true);

                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            } else {
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);
                scrollPane.setPannable(false);
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            }
        });
    }

    public double getViewportZoom() {
        return viewportZoom;
    }

    private double scrollSpeedX = 0;
    private double scrollSpeedY = 0;
    private javafx.animation.AnimationTimer scrollAnimator;
    private boolean scrollAnimatorRunning = false;
    private double lastMouseX = 0.5;
    private double lastMouseY = 0.5;

    public void scrollViewportSmooth(double relativeX, double relativeY) {
        if (disposed || viewportZoom <= 1.0) return;

        lastMouseX = relativeX;
        lastMouseY = relativeY;

        double edgeZone = 0.20;

        double baseSpeed = 0.025;
        double zoomBonus = Math.min((viewportZoom - 1.0) * 0.3, 1.0);
        double maxSpeed = baseSpeed * (1.0 + zoomBonus);

        if (relativeX < edgeZone) {

            double intensity = 1.0 - (relativeX / edgeZone);
            scrollSpeedX = -maxSpeed * Math.pow(intensity, 1.3);
        } else if (relativeX > (1.0 - edgeZone)) {

            double intensity = (relativeX - (1.0 - edgeZone)) / edgeZone;
            scrollSpeedX = maxSpeed * Math.pow(intensity, 1.3);
        } else if (relativeX <= 0) {

            scrollSpeedX = -maxSpeed * 1.5;
        } else if (relativeX >= 1.0) {

            scrollSpeedX = maxSpeed * 1.5;
        } else {

            scrollSpeedX = 0;
        }

        if (relativeY < edgeZone) {

            double intensity = 1.0 - (relativeY / edgeZone);
            scrollSpeedY = -maxSpeed * Math.pow(intensity, 1.3);
        } else if (relativeY > (1.0 - edgeZone)) {

            double intensity = (relativeY - (1.0 - edgeZone)) / edgeZone;
            scrollSpeedY = maxSpeed * Math.pow(intensity, 1.3);
        } else if (relativeY <= 0) {

            scrollSpeedY = -maxSpeed * 1.5;
        } else if (relativeY >= 1.0) {

            scrollSpeedY = maxSpeed * 1.5;
        } else {

            scrollSpeedY = 0;
        }

        if ((scrollSpeedX != 0 || scrollSpeedY != 0) && !scrollAnimatorRunning) {
            startSmoothScrollAnimation();
        }
    }

    public void updateScrollFromLastPosition() {
        scrollViewportSmooth(lastMouseX, lastMouseY);
    }

    private void startSmoothScrollAnimation() {
        if (scrollAnimator != null) {
            scrollAnimator.stop();
        }

        scrollAnimatorRunning = true;
        scrollAnimator = new javafx.animation.AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (disposed || viewportZoom <= 1.0) {
                    stopSmoothScrollAnimation();
                    return;
                }

                if (lastUpdate > 0 && (now - lastUpdate) < 16_000_000) {
                    return;
                }
                lastUpdate = now;

                double currentH = scrollPane.getHvalue();
                double currentV = scrollPane.getVvalue();

                double newH = currentH + scrollSpeedX;
                double newV = currentV + scrollSpeedY;

                newH = Math.max(0, Math.min(1, newH));
                newV = Math.max(0, Math.min(1, newV));

                scrollPane.setHvalue(newH);
                scrollPane.setVvalue(newV);

                if (scrollSpeedX == 0 && scrollSpeedY == 0) {
                    stopSmoothScrollAnimation();
                }
            }
        };
        scrollAnimator.start();
    }

    public void stopSmoothScrollAnimation() {
        scrollAnimatorRunning = false;
        scrollSpeedX = 0;
        scrollSpeedY = 0;
        if (scrollAnimator != null) {
            scrollAnimator.stop();
            scrollAnimator = null;
        }
    }

    public void scrollViewportTo(double relativeX, double relativeY) {
        if (disposed || viewportZoom <= 1.0) return;

        Platform.runLater(() -> {
            scrollPane.setHvalue(Math.max(0, Math.min(1, relativeX)));
            scrollPane.setVvalue(Math.max(0, Math.min(1, relativeY)));
        });
    }

    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    public void showDevTools() {

        logger.info("Developer tools not available in WebView mode");
    }

    public void showDevTools(String mode) {

        logger.info("Developer tools not available in WebView mode (mode: {})", mode);
    }

    public void viewSource() {
        if (disposed) return;
        String currentUrl = webEngine.getLocation();
        if (currentUrl != null && !currentUrl.startsWith("view-source:")) {
            loadUrl("view-source:" + currentUrl);
        }
    }

    public String getTitle() {
        return titleProperty.get();
    }

    public StringProperty titleProperty() {
        return titleProperty;
    }

    public String getUrl() {
        return urlProperty.get();
    }

    public StringProperty urlProperty() {
        return urlProperty;
    }

    public String getFaviconUrl() {
        return faviconUrlProperty.get();
    }

    public StringProperty faviconUrlProperty() {
        return faviconUrlProperty;
    }

    public Tab getTabModel() {
        return tabModel;
    }

    public void setTabModel(Tab tabModel) {
        this.tabModel = tabModel;
    }

    public boolean isLoading() {
        return webEngine.getLoadWorker().isRunning();
    }

    public WebView getWebView() {
        return webView;
    }

    public WebEngine getWebEngine() {
        return webEngine;
    }

    public Object executeScript(String script) {
        if (disposed || script == null || script.isEmpty()) return null;
        try {
            return webEngine.executeScript(script);
        } catch (Exception e) {
            logger.debug("Error executing script: {}", e.getMessage());
            return null;
        }
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;

        logger.info("Disposing BrowserTab: {}", getUrl());

        stopSmoothScrollAnimation();

        try {
            // First, cancel any ongoing loads
            try {
                webEngine.getLoadWorker().cancel();
            } catch (Exception ignore) {}

            // Remove all listeners
            if (viewportBoundsListener != null) scrollPane.viewportBoundsProperty().removeListener(viewportBoundsListener);
            if (titleChangeListener != null) webEngine.titleProperty().removeListener(titleChangeListener);
            if (locationChangeListener != null) webEngine.locationProperty().removeListener(locationChangeListener);
            if (stateChangeListener != null) webEngine.getLoadWorker().stateProperty().removeListener(stateChangeListener);
            if (progressChangeListener != null) webEngine.getLoadWorker().progressProperty().removeListener(progressChangeListener);
            if (exceptionChangeListener != null) webEngine.getLoadWorker().exceptionProperty().removeListener(exceptionChangeListener);

            // Clear event handlers
            this.setOnMouseMoved(null);
            this.setOnMouseDragged(null);
            this.setOnMouseExited(null);
            this.setOnKeyPressed(null);
            if (scrollPane != null) {
                scrollPane.setOnMouseMoved(null);
                scrollPane.setOnMouseDragged(null);
            }

            webEngine.setOnAlert(null);
            webEngine.setOnError(null);
            webEngine.setCreatePopupHandler(null);

            // Hide the WebView first to stop rendering
            webView.setVisible(false);

            // Load about:blank instead of null to properly clear resources
            try {
                webEngine.load("about:blank");
            } catch (Exception ignore) {}

            // Give the engine a moment to process the blank page
            Platform.runLater(() -> {
                try {
                    // Disconnect from scene graph
                    if (zoomGroup != null) {
                        zoomGroup.getChildren().clear();
                    }
                    if (scrollPane != null) {
                        scrollPane.setContent(null);
                    }
                    contentPane.getChildren().clear();

                    // Final cleanup
                    this.setCenter(null);
                    this.setTop(null);
                } catch (Exception e) {
                    logger.debug("Error in final cleanup phase", e);
                }
            });
        } catch (Exception e) {
            logger.error("Error cleaning up BrowserTab", e);
        }

        logger.info("BrowserTab disposed");
    }

    private void installDownloadBridge() {
        if (disposed) return;
        try {
            logger.info("Installing download bridge for page: {}", webEngine.getLocation());
            JSObject window = (JSObject) webEngine.executeScript("window");
            if (window == null) {
                logger.warn("Could not get window object for download bridge");
                return;
            }
            window.setMember("nexusDownloadBridge", new DownloadBridge());
            logger.info("DownloadBridge member set on window object");
            webEngine.executeScript(DOWNLOAD_INJECTION_JAVASCRIPT);
            logger.info("Download injection JavaScript executed successfully");
        } catch (Exception e) {
            logger.error("Failed to install download bridge", e);
        }
    }

    public class DownloadBridge {
        private SettingsService settingsService;

        public DownloadBridge() {
            try {
                this.settingsService = container.getOrCreate(SettingsService.class);
            } catch (Exception e) {
                logger.warn("Could not get SettingsService for DownloadBridge", e);
            }
        }

        public void download(String rawUrl) {
            if (disposed || rawUrl == null) {
                logger.warn("DownloadBridge.download called but disposed={} or rawUrl is null", disposed);
                return;
            }
            logger.info("DownloadBridge.download called with URL: {}", rawUrl);
            Platform.runLater(() -> {
                try {
                    String url = rawUrl.trim();
                    if (url.isEmpty()) {
                        logger.warn("DownloadBridge.download: URL is empty after trim");
                        return;
                    }
                    logger.info("Starting download for URL: {}", url);
                    DownloadController dc = container.getOrCreate(DownloadController.class);
                    String fileName = suggestFileNameFromUrl(url);
                    logger.info("Suggested filename: {}", fileName);
                    dc.requestDownloadFromUI(url, fileName);
                    logger.info("Download request submitted for: {}", url);
                } catch (Exception e) {
                    logger.error("DownloadBridge failed for {}", rawUrl, e);
                }
            });
        }

        public void downloadBlob(String base64Data, String filename, String mimeType) {
            if (disposed || base64Data == null) {
                logger.warn("DownloadBridge.downloadBlob called but disposed={} or data is null", disposed);
                return;
            }
            logger.info("DownloadBridge.downloadBlob called: filename={}, mimeType={}, dataLength={}",
                       filename, mimeType, base64Data.length());

            Platform.runLater(() -> {
                try {
                    // Decode base64 data
                    byte[] data = java.util.Base64.getDecoder().decode(base64Data);
                    logger.info("Decoded blob data, size: {} bytes", data.length);

                    // Determine save location
                    String downloadPath = settingsService != null ? settingsService.getDownloadPath() : null;
                    if (downloadPath == null || downloadPath.trim().isEmpty()) {
                        String userHome = System.getProperty("user.home", "");
                        downloadPath = userHome + java.io.File.separator + "Downloads";
                    }

                    // Create file with proper extension based on mime type
                    String finalFilename = filename;
                    if (!finalFilename.contains(".") && mimeType != null) {
                        String ext = getExtensionFromMimeType(mimeType);
                        if (ext != null) {
                            finalFilename += ext;
                        }
                    }

                    java.io.File targetFile = new java.io.File(downloadPath, finalFilename);

                    // Avoid overwriting existing files
                    if (targetFile.exists()) {
                        String base = finalFilename;
                        String ext = "";
                        int dotIdx = finalFilename.lastIndexOf('.');
                        if (dotIdx > 0) {
                            base = finalFilename.substring(0, dotIdx);
                            ext = finalFilename.substring(dotIdx);
                        }
                        int counter = 1;
                        while (targetFile.exists()) {
                            targetFile = new java.io.File(downloadPath, base + " (" + counter + ")" + ext);
                            counter++;
                        }
                    }

                    // Write data to file
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile)) {
                        fos.write(data);
                    }

                    logger.info("Blob download completed: {}", targetFile.getAbsolutePath());

                    // Show notification or update UI
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("Download Complete");
                    alert.setHeaderText("File Downloaded");
                    alert.setContentText("Saved to: " + targetFile.getAbsolutePath());
                    alert.show();

                } catch (Exception e) {
                    logger.error("DownloadBridge.downloadBlob failed", e);
                }
            });
        }

        private String getExtensionFromMimeType(String mimeType) {
            if (mimeType == null) return null;
            String lower = mimeType.toLowerCase();
            if (lower.contains("jpeg") || lower.contains("jpg")) return ".jpg";
            if (lower.contains("png")) return ".png";
            if (lower.contains("gif")) return ".gif";
            if (lower.contains("webp")) return ".webp";
            if (lower.contains("pdf")) return ".pdf";
            if (lower.contains("zip")) return ".zip";
            if (lower.contains("mp4")) return ".mp4";
            if (lower.contains("mp3")) return ".mp3";
            return null;
        }
    }

    private static final String DOWNLOAD_INJECTION_JAVASCRIPT = """
        (function() {
            if (!window.nexusDownloadBridge) {
                console.log('NexusDownloadBridge: Bridge not available');
                return;
            }
            console.log('NexusDownloadBridge: Installing download interceptor');
            
            // Global click interceptor - catch EVERYTHING
            document.addEventListener('click', function(event) {
                try {
                    var el = event.target;
                    var depth = 0;
                    
                    while (el && el !== document && depth < 15) {
                        // Check for download attribute
                        if (el.tagName === 'A' && el.hasAttribute('download') && el.href) {
                            console.log('NexusDownloadBridge: Download link clicked:', el.href);
                            event.preventDefault();
                            event.stopPropagation();
                            window.nexusDownloadBridge.download(el.href);
                            return false;
                        }
                        
                        // Check for any clickable element with download-related attributes or text
                        var attrs = ['data-download', 'data-download-url', 'data-url', 'data-href', 'data-src'];
                        for (var i = 0; i < attrs.length; i++) {
                            var attrVal = el.getAttribute(attrs[i]);
                            if (attrVal && attrVal.match(/^https?:\\/\\//)) {
                                console.log('NexusDownloadBridge: Found download URL in', attrs[i], ':', attrVal);
                                window.nexusDownloadBridge.download(attrVal);
                                event.preventDefault();
                                event.stopPropagation();
                                return false;
                            }
                        }
                        
                        // Check text content for download keywords
                        var text = (el.textContent || '').toLowerCase().trim();
                        var ariaLabel = (el.getAttribute('aria-label') || '').toLowerCase();
                        if ((text === 'download' || text.startsWith('download ') || ariaLabel.includes('download')) &&
                            (el.tagName === 'BUTTON' || el.tagName === 'A' || el.getAttribute('role') === 'button')) {
                            
                            console.log('NexusDownloadBridge: Download button clicked, searching for URL...');
                            
                            // Try to find image in context
                            var img = el.querySelector('img') || el.closest('div').querySelector('img');
                            if (img && img.src && img.src.match(/^https?:\\/\\//)) {
                                console.log('NexusDownloadBridge: Found image to download:', img.src);
                                window.nexusDownloadBridge.download(img.src);
                                event.preventDefault();
                                event.stopPropagation();
                                return false;
                            }
                            
                            // Try parent/sibling for link
                            var link = el.closest('a') || el.parentElement.querySelector('a');
                            if (link && link.href && link.href.match(/^https?:\\/\\//)) {
                                console.log('NexusDownloadBridge: Found related link:', link.href);
                                window.nexusDownloadBridge.download(link.href);
                                event.preventDefault();
                                event.stopPropagation();
                                return false;
                            }
                        }
                        
                        // Check if clicking on an image directly
                        if (el.tagName === 'IMG' && el.src && el.src.match(/^https?:\\/\\//)) {
                            var parent = el.parentElement;
                            // Only auto-download if parent has download intent
                            if (parent && (parent.hasAttribute('download') || 
                                          parent.getAttribute('data-download') || 
                                          (parent.textContent || '').toLowerCase().includes('download'))) {
                                console.log('NexusDownloadBridge: Image in download context clicked:', el.src);
                                window.nexusDownloadBridge.download(el.src);
                                event.preventDefault();
                                event.stopPropagation();
                                return false;
                            }
                        }
                        
                        el = el.parentElement;
                        depth++;
                    }
                } catch (e) {
                    console.error('NexusDownloadBridge error:', e);
                }
            }, true);
            
            console.log('NexusDownloadBridge: Download interceptor installed');
        })();
        """;

    private static final String DARK_MODE_CSS = """
        html {
            filter: invert(90%) hue-rotate(180deg) !important;
            background-color: #1a1a1a !important;
        }
        img, video, canvas, picture, svg, [style*="background-image"],
        iframe, embed, object {
            filter: invert(100%) hue-rotate(180deg) !important;
        }
        /* Fix specific elements that shouldn't be inverted */
        [data-darkreader-inline-bgcolor],
        [data-darkreader-inline-color] {
            filter: none !important;
        }

""";

    private static final String DARK_MODE_CSS_SOFT = """
         :root {
             color-scheme: dark !important;
         }
         html, body {
             background-color: #1e1e1e !important;
             color: #e0e0e0 !important;
         }
         * {
             background-color: inherit !important;
             color: inherit !important;
             border-color: #404040 !important;
         }
         a {
             color: #6db3f2 !important;
         }
         a:visited {
             color: #b794f4 !important;
         }
         img, video, canvas, picture, svg, iframe {
             opacity: 0.9 !important;
         }
         input, textarea, select, button {
             background-color: #2d2d2d !important;
             color: #e0e0e0 !important;
             border-color: #505050 !important;
         }
         """;


    public void enableWebPageDarkMode() {
        if (disposed) return;
        webPageDarkMode = true;
        injectDarkModeCSS();
    }

    public void disableWebPageDarkMode() {
        if (disposed) return;
        webPageDarkMode = false;
        removeDarkModeCSS();
    }

    public void toggleWebPageDarkMode() {
        if (webPageDarkMode) {
            disableWebPageDarkMode();
        } else {
            enableWebPageDarkMode();
        }
    }

    public boolean isWebPageDarkModeEnabled() {
        return webPageDarkMode;
    }

    public static void setGlobalDarkModeEnabled(boolean enabled) {
        globalDarkModeEnabled = enabled;
    }

    public static boolean isGlobalDarkModeEnabled() {
        return globalDarkModeEnabled;
    }

    private void injectDarkModeCSS() {
        if (disposed) return;

        Platform.runLater(() -> {
            try {
                String escapedCSS = DARK_MODE_CSS
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", " ")
                    .replace("\r", "");

                String script = """
                    (function() {
                        // Remove existing dark mode style if any
                        var existing = document.getElementById('nexus-dark-mode');
                        if (existing) existing.remove();

                        // Create and inject dark mode style
                        var style = document.createElement('style');
                        style.id = 'nexus-dark-mode';
                        style.type = 'text/css';
                        style.innerHTML = '%s';
                        document.head.appendChild(style);
                        return true;
                    })()
                    """.formatted(escapedCSS);

                webEngine.executeScript(script);
                logger.info("Dark mode CSS injected");
            } catch (Exception e) {
                logger.debug("Error injecting dark mode CSS: {}", e.getMessage());
            }
        });
    }

    private void removeDarkModeCSS() {
        if (disposed) return;

        Platform.runLater(() -> {
            try {
                String script = """
                    (function() {
                        var existing = document.getElementById('nexus-dark-mode');
                        if (existing) existing.remove();
                        return true;
                    })()
                    """;

                webEngine.executeScript(script);
                logger.info("Dark mode CSS removed");
            } catch (Exception e) {
                logger.debug("Error removing dark mode CSS: {}", e.getMessage());
            }
        });
    }

    private void reapplyDarkModeIfEnabled() {
        if (webPageDarkMode || globalDarkModeEnabled) {
            webPageDarkMode = true;

            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
            delay.setOnFinished(e -> injectDarkModeCSS());
            delay.play();
        }
    }

    private String suggestFileNameFromUrl(String urlStr) {
        try {
            URI u = new URI(urlStr);
            String path = u.getPath();
            if (path != null && !path.isEmpty()) {
                String last = path.substring(path.lastIndexOf('/') + 1);
                if (last == null || last.isEmpty()) last = "download";
                return java.net.URLDecoder.decode(last, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {

        }
        return "download";
    }

    private boolean shouldRunHeadCheck(String urlStr) {
        if (urlStr == null || urlStr.isEmpty()) return false;
        String lower = urlStr.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false;
        if (lower.startsWith("about:") || lower.startsWith("data:") || lower.startsWith("javascript:")) return false;
        return true;
    }

    private boolean isDirectImageUrl(String urlStr) {
        if (urlStr == null || urlStr.isEmpty()) return false;
        String lower = urlStr.toLowerCase();
        String path = lower.split("\\?")[0]; // Remove query params
        return path.matches(".*\\.(png|jpe?g|gif|bmp|webp|svg|ico)$");
    }

    private Consumer<String> openLinkInNewTabHandler;
    private Consumer<String> openLinkInNewWindowHandler;

    public void setOpenLinkInNewTabHandler(Consumer<String> handler) {
        this.openLinkInNewTabHandler = handler;
    }

    public void setOpenLinkInNewWindowHandler(Consumer<String> handler) {
        this.openLinkInNewWindowHandler = handler;
    }

    private void setupContextMenu() {
        webView.setOnContextMenuRequested(event -> handleContextMenu(event.getX(), event.getY(), event.getScreenX(), event.getScreenY()));
    }

    private void handleContextMenu(double x, double y, double screenX, double screenY) {
        logger.info("Context menu requested at x={}, y={}", x, y);

        String targetUrl = resolveContextUrl(x, y);
        String imageUrl = resolveImageUrl(x, y);

        logger.info("Resolved link URL: {}", targetUrl);
        logger.info("Resolved image URL: {}", imageUrl);

        // Show menu if we have any URL
        if (targetUrl == null && imageUrl == null) {
            logger.warn("No URL or image found at click position");
            return;
        }

        if (linkContextMenu != null && linkContextMenu.isShowing()) {
            linkContextMenu.hide();
        }

        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();

        // If we have an image, prioritize image operations
        if (imageUrl != null && !imageUrl.isEmpty()) {
            final String resolvedImageUrl = imageUrl;

            MenuItem downloadImage = new MenuItem("Download Image");
            downloadImage.setOnAction(e -> {
                logger.info("Context menu: Download Image clicked for: {}", resolvedImageUrl);
                try {
                    DownloadController dc = container.getOrCreate(DownloadController.class);
                    String fileName = suggestFileNameFromUrl(resolvedImageUrl);
                    logger.info("Requesting download via context menu: {} -> {}", resolvedImageUrl, fileName);
                    dc.requestDownloadFromUI(resolvedImageUrl, fileName);
                } catch (Exception ex) {
                    logger.error("Failed to download image from context menu", ex);
                }
            });

            MenuItem openImageInNewTab = new MenuItem("Open Image in New Tab");
            openImageInNewTab.setOnAction(e -> {
                logger.info("Context menu: Open Image in New Tab clicked for: {}", resolvedImageUrl);
                if (openLinkInNewTabHandler != null) {
                    openLinkInNewTabHandler.accept(resolvedImageUrl);
                } else {
                    logger.warn("openLinkInNewTabHandler is null");
                }
            });

            MenuItem copyImageUrl = new MenuItem("Copy Image Address");
            copyImageUrl.setOnAction(e -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(resolvedImageUrl);
                Clipboard.getSystemClipboard().setContent(content);
                logger.info("Copied image URL to clipboard: {}", resolvedImageUrl);
            });

            menu.getItems().addAll(downloadImage, openImageInNewTab, copyImageUrl);
        }

        // If there's also a link URL (and it's different from image), add link options
        if (targetUrl != null && !targetUrl.isEmpty() && !targetUrl.equals(imageUrl)) {
            final String resolvedUrl = targetUrl;

            if (!menu.getItems().isEmpty()) {
                menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
            }

            MenuItem openTab = new MenuItem("Open Link in New Tab");
            openTab.setOnAction(e -> {
                logger.info("Context menu: Open Link in New Tab clicked for: {}", resolvedUrl);
                if (openLinkInNewTabHandler != null) {
                    openLinkInNewTabHandler.accept(resolvedUrl);
                } else {
                    logger.warn("openLinkInNewTabHandler is null");
                }
            });

            MenuItem openWindow = new MenuItem("Open Link in New Window");
            openWindow.setOnAction(e -> {
                logger.info("Context menu: Open Link in New Window clicked for: {}", resolvedUrl);
                if (openLinkInNewWindowHandler != null) {
                    openLinkInNewWindowHandler.accept(resolvedUrl);
                } else {
                    logger.warn("openLinkInNewWindowHandler is null");
                }
            });

            MenuItem copyLink = new MenuItem("Copy Link Address");
            copyLink.setOnAction(e -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(resolvedUrl);
                Clipboard.getSystemClipboard().setContent(content);
                logger.info("Copied link URL to clipboard: {}", resolvedUrl);
            });

            menu.getItems().addAll(openTab, openWindow, copyLink);
        }

        if (menu.getItems().isEmpty()) {
            logger.warn("Context menu has no items, not showing");
            return;
        }

        linkContextMenu = menu;
        linkContextMenu.show(webView, screenX, screenY);
        logger.info("Context menu shown with {} items", menu.getItems().size());
    }

    private String resolveContextUrl(double x, double y) {
        String script = String.format(Locale.US, """
            (function() {
                var el = document.elementFromPoint(%f, %f);
                if (!el) return null;
                
                function resolve(href) {
                    try {
                        return new URL(href, document.baseURI).href;
                    } catch (err) {
                        return href;
                    }
                }
                
                function isImageUrl(url) {
                    if (!url) return false;
                    var lower = url.toLowerCase();
                    return lower.match(/\\.(jpg|jpeg|png|gif|webp|bmp|svg|ico)(\\?|$|#)/i);
                }
                
                // Look for links (but skip if they just point to images)
                var link = el.closest('a,area');
                if (link && link.href) {
                    var resolved = resolve(link.href);
                    // Only return link if it's NOT just an image URL
                    if (!isImageUrl(resolved)) {
                        return resolved;
                    }
                }
                
                // Check if element itself has href
                if (el.href && !isImageUrl(el.href)) {
                    return resolve(el.href);
                }
                
                return null;
            })()
            """, x, y);
        return normalizeLinkCandidate(executeHitTest(script));
    }

    private String resolveImageUrl(double x, double y) {
        String script = String.format(Locale.US, """
            (function() {
                var el = document.elementFromPoint(%f, %f);
                if (!el) return null;
                
                function isValidImageUrl(url) {
                    if (!url) return false;
                    // Skip data URLs for now (too large)
                    if (url.startsWith('data:')) return false;
                    // Skip blob URLs without proper handling
                    if (url.startsWith('blob:')) return false;
                    return url.startsWith('http://') || url.startsWith('https://');
                }
                
                // Direct image click - prioritize this
                if (el.tagName === 'IMG') {
                    // Try srcset first (higher quality)
                    if (el.srcset) {
                        var srcsetParts = el.srcset.split(',');
                        for (var i = srcsetParts.length - 1; i >= 0; i--) {
                            var parts = srcsetParts[i].trim().split(' ');
                            var url = parts[0];
                            if (isValidImageUrl(url)) return url;
                        }
                    }
                    // Try currentSrc (what's actually displayed)
                    if (el.currentSrc && isValidImageUrl(el.currentSrc)) {
                        return el.currentSrc;
                    }
                    // Fall back to src
                    if (el.src && isValidImageUrl(el.src)) {
                        return el.src;
                    }
                }
                
                // Look for images inside clicked element (within 3 levels)
                var imgs = el.querySelectorAll('img');
                for (var i = 0; i < imgs.length && i < 5; i++) {
                    var img = imgs[i];
                    if (img.currentSrc && isValidImageUrl(img.currentSrc)) {
                        return img.currentSrc;
                    }
                    if (img.src && isValidImageUrl(img.src)) {
                        return img.src;
                    }
                }
                
                // Check parent elements for images
                var parent = el.parentElement;
                var depth = 0;
                while (parent && depth < 3) {
                    var parentImg = parent.querySelector('img');
                    if (parentImg) {
                        if (parentImg.currentSrc && isValidImageUrl(parentImg.currentSrc)) {
                            return parentImg.currentSrc;
                        }
                        if (parentImg.src && isValidImageUrl(parentImg.src)) {
                            return parentImg.src;
                        }
                    }
                    parent = parent.parentElement;
                    depth++;
                }
                
                // Look for background image
                var current = el;
                depth = 0;
                while (current && depth < 5) {
                    var style = window.getComputedStyle(current);
                    var bgImage = style.backgroundImage;
                    if (bgImage && bgImage !== 'none') {
                        var match = bgImage.match(/url\\(['"]?([^'"\\)]+)['"]?\\)/);
                        if (match && isValidImageUrl(match[1])) {
                            return match[1];
                        }
                    }
                    current = current.parentElement;
                    depth++;
                }
                
                return null;
            })()
            """, x, y);
        return normalizeLinkCandidate(executeHitTest(script));
    }

    private Object executeHitTest(String script) {
        try {
            return webEngine.executeScript(script);
        } catch (Exception e) {
            logger.debug("Error running context-menu script: {} (" + e.getMessage() + ")", script);
            return null;
        }
    }

    private String normalizeLinkCandidate(Object raw) {
        if (raw == null) return null;
        String value = raw.toString();
        if (value == null) return null;
        value = value.trim();
        if (value.isEmpty()) return null;
        if ("null".equalsIgnoreCase(value) || "undefined".equalsIgnoreCase(value)) return null;
        return value;
    }
}
