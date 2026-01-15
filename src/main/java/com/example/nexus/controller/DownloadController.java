package com.example.nexus.controller;

import com.example.nexus.model.Download;
import com.example.nexus.service.DownloadService;
import com.example.nexus.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Locale;

import com.example.nexus.view.components.DownloadDropdown;
import javafx.scene.control.Button;
import com.example.nexus.core.DIContainer;
import com.example.nexus.view.components.DownloadManagerPanel;

public class DownloadController {
    private static final Logger logger = LoggerFactory.getLogger(DownloadController.class);

    private final DownloadService downloadService;
    private final SettingsService settingsService;
    private DownloadManagerPanel downloadManagerPanel; 

    public DownloadController(DownloadService downloadService, SettingsService settingsService) {
        this.downloadService = downloadService;
        this.settingsService = settingsService;
    }

    
    public void closePanel() {
        if (downloadManagerPanel != null && downloadManagerPanel.isShowing()) {
            downloadManagerPanel.close();
            downloadManagerPanel = null;
        }
    }

    public void startDownload(String url, String filename) {
        logger.info("startDownload called: url={}, filename={}", url, filename);
        try {
            String downloadPath = settingsService.getDownloadPath();
            logger.info("Download path from settings: {}", downloadPath);
            if (downloadPath == null || downloadPath.trim().isEmpty()) {
                String userHome = System.getProperty("user.home", "");
                downloadPath = userHome + java.io.File.separator + "Downloads";
                logger.info("Download path empty in settings; falling back to {}", downloadPath);
            }
            File targetDir = new File(downloadPath);
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    logger.warn("Could not create download directory: {}", targetDir.getAbsolutePath());
                } else {
                    logger.info("Created download directory: {}", targetDir.getAbsolutePath());
                }
            }

            File targetFile = new File(targetDir, filename);
            logger.info("Target file will be: {}", targetFile.getAbsolutePath());

            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) {
                boolean ok = parent.mkdirs();
                if (!ok) logger.warn("Failed to create parent directory for file: {}", parent.getAbsolutePath());
            }

            logger.info("Calling downloadService.startDownload for: {}", url);
            downloadService.startDownload(url, filename, targetFile.getAbsolutePath());
            logger.info("Download service call completed for: {} -> {}", url, targetFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error starting download: {}", url, e);
        }
    }

    public void startDownload(String url, String filename, String absolutePath) {
        try {
            downloadService.startDownload(url, filename, absolutePath);
            logger.info("Started download: {} -> {}", url, absolutePath);
        } catch (Exception e) {
            logger.error("Error starting download: {}", url, e);
        }
    }


    public List<Download> getAllDownloads() {
        return downloadService.getAllDownloads();
    }

    public void deleteDownload(int downloadId) {
        try {
            downloadService.deleteDownload(downloadId);
            logger.info("Deleted download: {}", downloadId);
        } catch (Exception e) {
            logger.error("Error deleting download: {}", downloadId, e);
        }
    }

    public void clearAllDownloads() {
        try {
            downloadService.clearDownloads();
            logger.info("Cleared all downloads");
        } catch (Exception e) {
            logger.error("Error clearing downloads", e);
        }
    }

    public void openDownloadLocation(Download download) {
        if (download != null && download.getFilePath() != null) {
            try {
                File file = new File(download.getFilePath());
                if (file.exists()) {
                    openWithSystem(file.getParentFile());
                }
            } catch (Exception e) {
                logger.error("Error opening download location", e);
            }
        }
    }

    public void openDownloadFile(Download download) {
        if (download != null && download.getFilePath() != null) {
            try {
                File file = new File(download.getFilePath());
                if (file.exists()) {
                    openWithSystem(file);
                } else {

                    openDownloadLocation(download);
                }
            } catch (Exception e) {
                logger.error("Error opening download file", e);
            }
        }
    }

    private void openWithSystem(File fileOrParent) {
        if (fileOrParent == null) return;
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(fileOrParent);
                return;
            }
        } catch (Exception desktopEx) {
            logger.debug("Desktop open failed", desktopEx);
        }

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
        ProcessBuilder builder;
        if (os.contains("win")) {
            
            builder = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", fileOrParent.toURI().toString());
        } else if (os.contains("mac")) {
            builder = new ProcessBuilder("open", fileOrParent.getAbsolutePath());
        } else {
            
            builder = new ProcessBuilder("xdg-open", fileOrParent.getAbsolutePath());
        }
        try {
            builder.start();
        } catch (Exception e) {
            logger.error("Failed to open file or directory: {}", fileOrParent, e);
        }
    }

    public boolean shouldAskDownloadLocation() {
        return settingsService.isAskDownloadLocation();
    }

    public void requestDownloadFromUI(String url, String suggestedFileName) {
        logger.info("requestDownloadFromUI called: url={}, fileName={}", url, suggestedFileName);
        try {
            
            
            logger.info("Starting automatic download (bypassing file chooser for debugging)");
            startDownload(url, suggestedFileName);
            logger.info("Download request processed successfully for: {}", url);
        } catch (Exception e) {
            logger.error("Error requesting download from UI: {}", url, e);
        }
    }

    public DownloadDropdown createDownloadDropdown(DownloadService ds, boolean isDarkTheme, Button downloadsButton) {
        DownloadDropdown downloadDropdown = new DownloadDropdown(ds, this, isDarkTheme, () -> {
            try { showDownloadManagerPanel(null, null, null); } catch (Exception ex) { logger.warn("Failed to open downloads panel", ex); }
        });
        final java.util.concurrent.atomic.AtomicBoolean pendingUiUpdate = new java.util.concurrent.atomic.AtomicBoolean(false);
        ds.addListener(new com.example.nexus.service.DownloadListener() {
            @Override
            public void downloadAdded(com.example.nexus.model.Download download) {
                // coalesce rapid events into a single UI update
                if (pendingUiUpdate.compareAndSet(false, true)) {
                    javafx.application.Platform.runLater(() -> {
                        try {
                            updateDownloadsBadge(null, downloadsButton);
                            if (downloadsButton != null && downloadsButton.getScene() != null) {
                                var bounds = downloadsButton.localToScene(downloadsButton.getBoundsInLocal());
                                javafx.stage.Window w = downloadsButton.getScene().getWindow();
                                downloadDropdown.showNear(w, bounds);
                            }
                        } catch (Exception ignored) {}
                        finally { pendingUiUpdate.set(false); }
                    });
                }
            }
            @Override
            public void downloadUpdated(com.example.nexus.model.Download download) {
                if (pendingUiUpdate.compareAndSet(false, true)) {
                    javafx.application.Platform.runLater(() -> {
                        try {
                            downloadDropdown.refreshContent();
                            updateDownloadsBadge(null, downloadsButton);
                        } catch (Exception ignored) {}
                        finally { pendingUiUpdate.set(false); }
                    });
                }
            }
        });
        return downloadDropdown;
    }

    public void showDownloadManagerPanel(DIContainer container, SettingsService settingsService, com.example.nexus.controller.SettingsController settingsController) {
        showDownloadManagerPanel(container, settingsService, settingsController, null);
    }

    public void showDownloadManagerPanel(DIContainer container, SettingsService settingsService, com.example.nexus.controller.SettingsController settingsController, com.example.nexus.util.KeyboardShortcutManager shortcutManager) {
        
        if (downloadManagerPanel != null && downloadManagerPanel.isShowing()) {
            downloadManagerPanel.toFront();
            downloadManagerPanel.requestFocus();
            logger.info("Download manager already open, bringing to front");
            return;
        }

        boolean isDarkTheme = false;
        if (settingsService != null && settingsController != null) {
            isDarkTheme = "dark".equals(settingsService.getTheme()) || ("system".equals(settingsService.getTheme()) && settingsController.isSystemDark());
        }
        downloadManagerPanel = new DownloadManagerPanel(container, this, isDarkTheme);

        
        downloadManagerPanel.setOnHidden(e -> downloadManagerPanel = null);

        downloadManagerPanel.show();

        if (shortcutManager != null && downloadManagerPanel.getScene() != null) {
            try {
                shortcutManager.setupForScene(downloadManagerPanel.getScene());
            } catch (Exception ignored) {}
        }
    }

    public void toggleDownloadDropdown(DownloadDropdown downloadDropdown, Button downloadsButton) {
        if (downloadDropdown != null && downloadsButton != null && downloadsButton.getScene() != null) {
            javafx.geometry.Bounds b = downloadsButton.localToScene(downloadsButton.getBoundsInLocal());
            javafx.stage.Window w = downloadsButton.getScene().getWindow();
            if (downloadDropdown.isShowing()) {
                downloadDropdown.hide();
            } else {
                downloadDropdown.showNear(w, b);
            }
        } else {
            showDownloadManagerPanel(null, null, null);
        }
    }

    public void updateDownloadsBadge(DIContainer container, Button downloadsButton) {
        try {
            DownloadService ds = (container != null) ? container.getOrCreate(DownloadService.class) : this.downloadService;
            long active = ds.getAllDownloads().stream().filter(d -> "downloading".equals(d.getStatus())).count();
            javafx.application.Platform.runLater(() -> {
                if (downloadsButton != null) {
                    if (active > 0) {
                        downloadsButton.setText(String.valueOf(active));
                        downloadsButton.getStyleClass().remove("badge-hidden");
                        downloadsButton.getStyleClass().add("badge-visible");
                    } else {
                        downloadsButton.setText("");
                        downloadsButton.getStyleClass().remove("badge-visible");
                        downloadsButton.getStyleClass().add("badge-hidden");
                    }
                }
            });
        } catch (Exception ignored) {}
    }
}
