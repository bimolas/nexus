package com.example.nexus.controller;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Profile;
import com.example.nexus.service.*;
import com.example.nexus.util.KeyboardShortcutManager;
import com.example.nexus.view.MainView;
import com.example.nexus.view.components.BookmarkBarComponent;
import com.example.nexus.view.components.BrowserTab;
import com.example.nexus.view.components.DownloadDropdown;
import com.example.nexus.view.components.StatusBarComponent;
import com.example.nexus.view.dialogs.SettingsPanel;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private DIContainer container;
    private TabService tabService;
    private SettingsService settingsService;
    private SettingsController settingsController;
    private KeyboardShortcutManager shortcutManager;
    private HistoryController historyController;
    private BookmarkController bookmarkController;
    private ZoomService zoomService;
    private TabController tabController;
    private MenuController menuController;
    private NavigationController navController;
    private BookmarkBarComponent bookmarkBarComponent;
    private StatusBarComponent statusBarComponent;

    @FXML private BorderPane rootPane;
    @FXML private TabPane tabPane;
    @FXML private StackPane browserContainer;
    @FXML private TextField addressBar;
    @FXML private Button backButton;
    @FXML private Button forwardButton;
    @FXML private Button reloadButton;
    @FXML private Button homeButton;
    @FXML private Button newTabButton;
    @FXML private Button bookmarkButton;
    @FXML private Button downloadsButton;
    @FXML private Button profileButton;
    @FXML private Button menuButton;
    @FXML private Label securityIcon;
    @FXML private HBox statusBar;

    private DownloadDropdown downloadDropdown;
    private DownloadController downloadController;

    
    private Stage settingsStage;
    private Stage bookmarkStage;
    private Stage historyStage;
    private Stage downloadStage;
    private Stage profileStage;

    // New: allow a MainController instance to suppress automatic session restore
    // (used when creating a new window that should start with a fresh tab)
    private boolean suppressSessionRestore = false;
    private String initialTabUrl = null;

    public void setContainer(DIContainer container) {
        this.container = container;
    }
    public DIContainer getContainer() {
        return container;
    }

    // New setters used by MainView controller factory
    public void setSuppressSessionRestore(boolean suppress) {
        this.suppressSessionRestore = suppress;
    }

    public void setInitialTabUrl(String url) {
        this.initialTabUrl = url;
    }

    // New helper to allow external code to ask this controller to open a URL in its window
    public void createNewTabInThisWindow(String url) {
        try {
            if (tabController != null) {
                tabController.createNewTab(url);
            }
        } catch (Exception e) {
            logger.error("Failed to create new tab in window", e);
        }
    }


    @FXML
    public void initialize() {

        tabService = container.getOrCreate(TabService.class);
        BookmarkService bookmarkService = container.getOrCreate(BookmarkService.class);
        HistoryService historyService = container.getOrCreate(HistoryService.class);
        settingsService = container.getOrCreate(SettingsService.class);
        shortcutManager = new com.example.nexus.util.KeyboardShortcutManager();

        try {

            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.T, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleNewTab);
            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.W, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleCloseTab);

            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.R, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleReload);
            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.R, javafx.scene.input.KeyCombination.CONTROL_DOWN, javafx.scene.input.KeyCombination.SHIFT_DOWN), this::handleReload);

            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.EQUALS, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleZoomIn);
            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.EQUALS, javafx.scene.input.KeyCombination.CONTROL_DOWN, javafx.scene.input.KeyCombination.SHIFT_DOWN), this::handleZoomIn);
            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.ADD, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleZoomIn);

            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.MINUS, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleZoomOut);
            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.SUBTRACT, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleZoomOut);

            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.DIGIT0, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleZoomReset);
            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.NUMPAD0, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleZoomReset);

            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.H, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleShowHistory);
            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.J, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleShowDownloads);
            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.B, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleShowBookmarks);
            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.D, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleBookmarkCurrentPage);

            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.M, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::handleToggleMagnifierMode);
            shortcutManager.addShortcut(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.M, javafx.scene.input.KeyCombination.CONTROL_DOWN, javafx.scene.input.KeyCombination.SHIFT_DOWN), this::handleToggleMouseNavigation);
        } catch (Exception e) {
            logger.debug("Failed to register some default shortcuts", e);
        }

        if (rootPane != null && rootPane.getScene() != null) {
            shortcutManager.setupForScene(rootPane.getScene());
        }

        historyController = new HistoryController(historyService);
        bookmarkController = new BookmarkController(bookmarkService, settingsService);
        tabController = new TabController(container);

        
        // Wire link open handlers. New-window handler will create a fresh window (no session restore)
        tabController.setLinkOpenHandlers(
            url -> tabController.createNewTab(url),  
            url -> {
                try {
                    logger.info("Opening link in new window: {}", url);
                    Platform.runLater(() -> {
                        try {
                            Stage newWindow = new Stage();
                            newWindow.setTitle("Nexus Browser");

                            MainView mainView = new MainView(container, true, url);

                            Scene scene = new Scene(mainView, 1280, 800);
                            var mainCssResource = getClass().getResource("/com/example/nexus/css/main.css");
                            if (mainCssResource != null) {
                                scene.getStylesheets().add(mainCssResource.toExternalForm());
                            }

                            String currentTheme = settingsService.getTheme();
                            String actualTheme = settingsController.resolveTheme(currentTheme);
                            var themeCssResource = getClass().getResource("/com/example/nexus/css/" + actualTheme + ".css");
                            if (themeCssResource != null) {
                                scene.getStylesheets().add(themeCssResource.toExternalForm());
                            }

                            // reuse current settings controller to register scene so theme changes propagate
                            try { settingsController.registerScene(scene); } catch (Exception ignored) {}

                            newWindow.setScene(scene);
                            newWindow.setMinWidth(800);
                            newWindow.setMinHeight(600);

                            try {
                                var iconStream = getClass().getResourceAsStream("/com/example/nexus/icons/browser.png");
                                if (iconStream != null) {
                                    newWindow.getIcons().add(new javafx.scene.image.Image(iconStream));
                                }
                            } catch (Exception e) {
                                logger.debug("Could not load browser icon for new window");
                            }

                            newWindow.show();

                            logger.info("New browser window opened for link: {}", url);
                        } catch (Exception e) {
                            logger.error("Failed to open link in new window", e);
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error handling open link in new window", e);
                }
            }
        );

        settingsController = new SettingsController(settingsService);
        settingsController.setUIComponents(rootPane, addressBar, tabPane, browserContainer, statusBar);

        bookmarkController.addBookmarkChangeListener(this::refreshBookmarkBar);

        settingsService.addSettingsChangeListener(settings -> {
            Platform.runLater(() -> {
                applyInterfaceSettings();

                refreshBookmarkBar();
            });
        });

        if (container != null) {
            zoomService = container.getOrCreate(ZoomService.class);
        }
        if (zoomService == null) {
            zoomService = new ZoomService();
        }

        setupIcons();
        menuController = new MenuController(
                this,
                settingsService,
                zoomService,
                tabController
        );
        navController = new NavigationController(tabController, settingsService);

        downloadController = container.getOrCreate(DownloadController.class);
        tabController.initializeUIComponents(tabPane, browserContainer, addressBar, securityBarSafe());

        try {
            var ds = container.getOrCreate(com.example.nexus.service.DownloadService.class);
            boolean isDarkTheme = "dark".equals(settingsService.getTheme()) || ("system".equals(settingsService.getTheme()) && settingsController.isSystemDark());
            downloadDropdown = downloadController.createDownloadDropdown(ds, isDarkTheme, downloadsButton);
        } catch (Exception e) {
            logger.warn("Could not create download dropdown", e);
        }

        bookmarkBarComponent = new BookmarkBarComponent(
                bookmarkService,
                settingsService,
                navController::navigateToUrl,
                url -> tabController.createNewTab(url),
                url -> bookmarkController.showBookmarkPanel(container, tabController::createNewTab)
        );

        statusBarComponent = new StatusBarComponent(settingsService);

        Node currentTop = rootPane.getTop();

        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(currentTop, bookmarkBarComponent);

        rootPane.setTop(topContainer);

        rootPane.setBottom(statusBarComponent);

        addressBar.setOnAction(e -> navController.navigateToUrl(addressBar.getText()));

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                tabController.showBrowserForTab(newTab);

                BrowserTab browserTab = tabController.getBrowserTabForFxTab(newTab);
                if (browserTab != null) {

                    statusBarComponent.setUrl(browserTab.getUrl());

                    browserTab.getWebEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
                        if (newState == Worker.State.RUNNING || newState == Worker.State.SCHEDULED) {
                            statusBarComponent.setStatus("Loading...");
                        } else if (newState == Worker.State.SUCCEEDED || newState == Worker.State.CANCELLED) {
                            statusBarComponent.setStatus("Ready");
                        }
                    });

                    browserTab.urlProperty().addListener((ov, oldUrl, newUrl) -> {
                        if (newUrl != null && !newUrl.isEmpty()) {
                            statusBarComponent.setUrl(newUrl);
                        }
                    });

                    browserTab.getWebView().zoomProperty().addListener((ov, oldZoom, newZoom) -> {
                        statusBarComponent.setZoom((int) (newZoom.doubleValue() * 100));
                    });
                }
            }
        });

        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabPane.setFocusTraversable(false);

        Platform.runLater(() -> tabController.hideTabDropdownButton());

        Platform.runLater(() -> {
            String savedTheme = settingsService.getTheme();
            settingsController.applyTheme(savedTheme != null ? savedTheme : "light");
            settingsController.applyInterfaceSettings();

            // Initialize sessions or create a fresh tab depending on suppression flag
            initializeSessionPersistence();
        });

        settingsService.addSettingsChangeListener(settings -> {
            Platform.runLater(() -> settingsController.applyInterfaceSettings());
        });

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                shortcutManager.setupForScene(newScene);

                
                javafx.stage.Window window = newScene.getWindow();
                if (window != null) {
                    window.setOnCloseRequest(event -> saveSessionOnExit());
                } else {
                    
                    newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                        if (newWindow != null) {
                            newWindow.setOnCloseRequest(event -> saveSessionOnExit());
                        }
                    });
                }
            }
        });

    }

    private Label securityBarSafe() {
        return securityIcon;
    }

    private void setupIcons() {

        backButton.setGraphic(safeIcon("mdi2a-arrow-left", 18));
        forwardButton.setGraphic(safeIcon("mdi2a-arrow-right", 18));
        reloadButton.setGraphic(safeIcon("mdi2r-refresh", 18));
        homeButton.setGraphic(safeIcon("mdi2h-home", 18));
        newTabButton.setGraphic(safeIcon("mdi2p-plus", 18));
        menuButton.setGraphic(safeIcon("mdi2m-menu", 18));

        if (bookmarkButton != null) {
            bookmarkButton.setGraphic(safeIcon("mdi2b-bookmark-outline", 16));
        }
        if (downloadsButton != null) {
            downloadsButton.setGraphic(safeIcon("mdi2d-download", 16));

            downloadsButton.getProperties().put("downloadBadge", new javafx.scene.control.Label(""));
        }
        if (profileButton != null) {
            profileButton.setGraphic(safeIcon("mdi2a-account-circle-outline", 16));
        }
        if (securityIcon != null) {
            securityIcon.setGraphic(safeIcon("mdi2l-lock", 14));
        }
    }

    private javafx.scene.Node safeIcon(String literal, int size) {
        try {
            FontIcon fi = new FontIcon(literal);
            fi.setIconSize(size);
            return fi;
        } catch (Throwable t) {
            Label l = new Label("?");
            l.setStyle("-fx-font-size: " + Math.max(12, size-2) + "px; -fx-text-fill: #495057;");
            return l;
        }
    }

    @FXML
    private void handleBack() {
        navController.goBack();
    }

    @FXML
    private void handleForward() {
        navController.goForward();
    }

    @FXML
    public void handleReload() {
        navController.reload();
    }

    @FXML
    private void handleHome() {
        navController.goHome();
    }

    @FXML
    public void handleNewTab() {
        tabController.createNewTab(settingsService.getHomePage());
    }
    @FXML
    private void handleMenu() {
        menuController.showMainMenu(menuButton);

    }

    public void handleZoomIn() {
        BrowserTab tab = tabController.getCurrentBrowserTab();
        if (tab == null) return;
        zoomService.zoomIn(tab, () -> {
            if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                zoomService.showZoomNotification((Stage) rootPane.getScene().getWindow());
            }
        });
    }

    public void handleZoomOut() {
        BrowserTab tab = tabController.getCurrentBrowserTab();
        if (tab == null) return;
        zoomService.zoomOut(tab, () -> {
            if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                zoomService.showZoomNotification((Stage) rootPane.getScene().getWindow());
            }
        });
    }

    public void handleZoomReset() {
        BrowserTab tab = tabController.getCurrentBrowserTab();
        if (tab == null) return;
        zoomService.resetZoom(tab, () -> {
            if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                zoomService.showZoomNotification((Stage) rootPane.getScene().getWindow());
            }
        });
    }

    @FXML
    public void handleBookmarkCurrentPage() {
        BrowserTab currentTab = tabController.getCurrentBrowserTab();
        if (currentTab != null) {
            String url = currentTab.getUrl();
            String title = currentTab.getTitle();
            bookmarkController.toggleBookmark(url, title, bookmarkButton);
        }
    }

    @FXML
    public void handleShowDownloads() {
        try {
            downloadController.showDownloadManagerPanel(container, settingsService, settingsController, shortcutManager);
        } catch (Exception e) {
            logger.error("Error opening download manager panel", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open Download Manager");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleDownloadsButtonClicked() {
        try {
            downloadController.toggleDownloadDropdown(downloadDropdown, downloadsButton);
        } catch (Exception e) {
            logger.warn("Error showing download dropdown", e);
            handleShowDownloads();
        }
    }

    @FXML
    public void handleShowHistory() {
        try {
            historyController.showHistoryPanel(container, tabController::createNewTab, shortcutManager);
        } catch (Exception e) {
            logger.error("Error opening history panel", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open history");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleShowBookmarks() {
        try {
            bookmarkController.showBookmarkPanel(container, tabController::createNewTab, shortcutManager);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open bookmarks");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleShowSettings() {
        try {
            
            if (settingsStage != null && settingsStage.isShowing()) {
                settingsStage.toFront();
                settingsStage.requestFocus();
                logger.info("Settings window already open, bringing to front");
                return;
            }

            SettingsPanel settingsPanel = new SettingsPanel(settingsService);

            settingsStage = new javafx.stage.Stage();
            settingsStage.setTitle("Settings");
            settingsStage.initModality(javafx.stage.Modality.NONE);

            javafx.scene.Scene settingsScene = new javafx.scene.Scene(settingsPanel, 1000, 750);
            var mainCssResource = getClass().getResource("/com/example/nexus/css/main.css");
            if (mainCssResource != null) {
                settingsScene.getStylesheets().add(mainCssResource.toExternalForm());
            }

            String currentTheme = settingsService.getTheme();
            String actualTheme = settingsController.resolveTheme(currentTheme);
            var themeCssResource = getClass().getResource("/com/example/nexus/css/" + actualTheme + ".css");
            if (themeCssResource != null) {
                settingsScene.getStylesheets().add(themeCssResource.toExternalForm());
            }

            settingsStage.setScene(settingsScene);
            settingsStage.setMinWidth(750);
            settingsStage.setMinHeight(600);
            settingsStage.setResizable(true);

            settingsController.registerScene(settingsScene);

            settingsPanel.setOnThemeChange(theme -> {
                settingsService.setTheme(theme);
            });

            settingsStage.setOnHidden(e -> {
                settingsController.unregisterScene(settingsScene);
                settingsStage = null; 
            });

            settingsStage.show();
            settingsStage.toFront();

        } catch (Exception e) {
            logger.error("Error showing settings", e);
        }
    }


    public void applyInterfaceSettings() {
        settingsController.applyInterfaceSettings();
    }


    public void refreshBookmarkBar() {
        if (bookmarkBarComponent != null) {
            bookmarkBarComponent.refresh();
        }
    }


    @FXML
    public void handleNewWindow() {
        try {
            logger.info("Opening new browser window");

            
            Stage newWindow = new Stage();
            newWindow.setTitle("Nexus Browser");

            
            try {
                var iconStream = getClass().getResourceAsStream("/com/example/nexus/icons/browser.png");
                if (iconStream != null) {
                    newWindow.getIcons().add(new javafx.scene.image.Image(iconStream));
                }
            } catch (Exception e) {
                logger.debug("Could not load browser icon for new window");
            }

            MainView mainView = new MainView(container, true, null);

            Scene scene = new Scene(mainView, 1280, 800);
            var mainCssResource = getClass().getResource("/com/example/nexus/css/main.css");
            if (mainCssResource != null) {
                scene.getStylesheets().add(mainCssResource.toExternalForm());
            }

            String currentTheme = settingsService.getTheme();
            String actualTheme = settingsController.resolveTheme(currentTheme);
            var themeCssResource = getClass().getResource("/com/example/nexus/css/" + actualTheme + ".css");
            if (themeCssResource != null) {
                scene.getStylesheets().add(themeCssResource.toExternalForm());
            }

            settingsController.registerScene(scene);
            newWindow.setOnHidden(e -> {
                settingsController.unregisterScene(scene);
                logger.info("Browser window closed");
            });

            newWindow.setScene(scene);
            newWindow.setMinWidth(800);
            newWindow.setMinHeight(600);

            
            newWindow.show();

            logger.info("New browser window opened successfully");

        } catch (Exception e) {
            logger.error("Error opening new browser window", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open new window");
            alert.setContentText("Could not create a new browser window: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Nexus Browser");
        alert.setHeaderText("Nexus Browser v1.0.0");
        alert.setContentText("A modern web browser built with JavaFX and JCEF.\n\n© 2024");
        alert.showAndWait();
    }

    @FXML
    public void handleExit() {
        saveCurrentSession();
        Platform.exit();
    }

    @FXML
    public void handleFind() {
        showNotImplementedAlert("Find");
    }

    @FXML
    public void handlePrint() {
        BrowserTab currentTab = tabController.getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.print();
        }
    }

    @FXML
    public void handleFullScreen() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    @FXML
    public void handleShowDeveloperTools() {
        BrowserTab currentTab = tabController.getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.showDevTools();
        }
    }

    @FXML
    public void handleDuplicateTab() {
        BrowserTab currentTab = tabController.getCurrentBrowserTab();
        if (currentTab != null) {
            tabController.createNewTab(currentTab.getUrl());
        }
    }

    @FXML
    public void handlePinTab() {
        showNotImplementedAlert("Pin Tab");
    }

    @FXML
    public void handleGroupTab() {
        showNotImplementedAlert("Group Tab");
    }

    @FXML
    public void handleCloseTab() {
        navController.closeCurrentTab();
    }

    @FXML
    public void handleNewPrivateWindow() {
        showNotImplementedAlert("Private Window");
    }

    private void showNotImplementedAlert(String feature) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText(feature);
        alert.setContentText("This feature is not yet implemented.");
        alert.showAndWait();
    }

    public void saveCurrentSession() {
        logger.info("saveCurrentSession() called from BrowserApplication.stop()");
        saveSessionOnExit();
    }
    public void handleToggleMagnifierMode() {
        if (zoomService != null) {
            zoomService.toggleZoomMode(() -> {
                if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                    zoomService.showZoomModeNotification((Stage) rootPane.getScene().getWindow());
                }
            });
        }
    }

    public void handleToggleMouseNavigation() {
        if (zoomService != null) {
            zoomService.toggleMouseTracking(() -> {
                if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                    zoomService.showMouseTrackingNotification((Stage) rootPane.getScene().getWindow());
                }
            });
        }
    }

    public KeyboardShortcutManager getShortcutManager() {
        return shortcutManager;
    }


    private void restoreUserData() {
        try {

            com.example.nexus.service.ProfileService profileService = container.getOrCreate(com.example.nexus.service.ProfileService.class);
            profileService.getCurrentProfile();

        } catch (Exception e) {
            logger.error("Error restoring user data", e);
        }
    }

    private void saveAllUserData() {
        try {

            com.example.nexus.service.ProfileService profileService = container.getOrCreate(com.example.nexus.service.ProfileService.class);
            profileService.saveProfile(profileService.getCurrentProfile());

        } catch (Exception e) {
            logger.error("Error saving user data on exit", e);
        }
    }

    @FXML
    public void handleShowProfile() {
        try {
            
            if (profileStage != null && profileStage.isShowing()) {
                profileStage.toFront();
                profileStage.requestFocus();
                logger.info("Profile window already open, bringing to front");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/nexus/fxml/dialogs/profile-panel.fxml"));
            loader.setControllerFactory(controllerClass -> {
                if (controllerClass == ProfileController.class) {
                    return new ProfileController(container);
                }
                try {
                    return controllerClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            javafx.scene.layout.VBox profileRoot = loader.load();
            profileStage = new javafx.stage.Stage();
            profileStage.setTitle("Profile");
            javafx.scene.Scene profileScene = new javafx.scene.Scene(profileRoot);

            var mainCssResource = getClass().getResource("/com/example/nexus/css/main.css");
            if (mainCssResource != null) {
                profileScene.getStylesheets().add(mainCssResource.toExternalForm());
            }
            String currentTheme = settingsService.getTheme();
            String actualTheme = settingsController.resolveTheme(currentTheme);
            var themeCssResource = getClass().getResource("/com/example/nexus/css/" + actualTheme + ".css");
            if (themeCssResource != null) {
                profileScene.getStylesheets().add(themeCssResource.toExternalForm());
            }

            profileStage.setScene(profileScene);
            profileStage.setResizable(false);
            settingsController.registerScene(profileScene);
            profileStage.setOnHidden(e -> {
                settingsController.unregisterScene(profileScene);
                profileStage = null; 
            });

            profileStage.show();
            profileStage.toFront();

        } catch (Exception e) {
            logger.error("Error opening profile panel", e);
        }
    }

    

    private void initializeSessionPersistence() {
        try {
            // If this MainController was created with suppression enabled, open a single new tab
            if (suppressSessionRestore) {
                logger.info("Suppressing session restore for this window and opening a single tab");
                Platform.runLater(() -> tabController.createNewTab(initialTabUrl != null ? initialTabUrl : settingsService.getHomePage()));
                return;
            }

            ProfileService profileService = container.getOrCreate(ProfileService.class);
            Profile currentProfile = profileService.getCurrentProfile();

            if (currentProfile == null || currentProfile.getId() <= 0) {
                logger.warn("No valid profile found, creating default tab");
                Platform.runLater(() -> tabController.createNewTab(settingsService.getHomePage()));
                return;
            }

            
            boolean restoreSession = settingsService.isRestoreSession();

            if (restoreSession && tabController.hasSessionForProfile(currentProfile.getId())) {
                logger.info("Restoring session for profile: {}", currentProfile.getUsername());
                Platform.runLater(() -> tabController.restoreSessionForProfile(currentProfile.getId()));
            } else {
                logger.info("Session restore disabled or no saved session, creating default tab");
                
                TabService tabService = container.getOrCreate(TabService.class);
                tabService.clearProfileTabs(currentProfile.getId());
                Platform.runLater(() -> tabController.createNewTab(settingsService.getHomePage()));
            }

        } catch (Exception e) {
            logger.error("Error initializing session persistence", e);
            
            Platform.runLater(() -> tabController.createNewTab(settingsService.getHomePage()));
        }
    }

    
    private void saveSessionOnExit() {
        logger.info("saveSessionOnExit() called - saving current session");
        try {
            ProfileService profileService = container.getOrCreate(ProfileService.class);
            Profile currentProfile = profileService.getCurrentProfile();

            if (currentProfile == null || currentProfile.getId() <= 0) {
                logger.warn("No valid profile to save session for");
                return;
            }

            logger.info("Saving session for profile: {} (ID: {})", currentProfile.getUsername(), currentProfile.getId());

            
            boolean restoreSession = settingsService.isRestoreSession();
            logger.info("Restore session setting: {}", restoreSession);

            if (restoreSession) {
                logger.info("Session restore enabled, saving tabs for profile {}", currentProfile.getId());
                tabController.saveSessionForProfile(currentProfile.getId());
            } else {
                logger.info("Session restore disabled, clearing saved tabs");
                
                TabService tabService = container.getOrCreate(TabService.class);
                tabService.clearProfileTabs(currentProfile.getId());
            }

            
            closeAllPanels();

        } catch (Exception e) {
            logger.error("Error saving session on exit", e);
        }
    }

    
    private void closeAllPanels() {
        logger.info("Closing all open panels");

        if (settingsStage != null && settingsStage.isShowing()) {
            settingsStage.close();
            settingsStage = null;
        }

        if (profileStage != null && profileStage.isShowing()) {
            profileStage.close();
            profileStage = null;
        }

        
        if (bookmarkController != null) {
            bookmarkController.closePanel();
        }

        
        if (historyController != null) {
            historyController.closePanel();
        }

        
        if (downloadController != null) {
            downloadController.closePanel();
        }
    }
}
