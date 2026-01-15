# 🌐 Nexus Browser

A modern, feature-rich web browser built with **JavaFX** and **JCEF (Java Chromium Embedded Framework)**. Nexus Browser provides a Chrome-like browsing experience with a beautiful, customizable interface.

![Java](https:
![JavaFX](https:
![JCEF](https:
![SQLite](https:
![License](https:

---

## 📋 Table of Contents

- [Features](#-features)
- [Screenshots](#-screenshots)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Project Structure](#-project-structure)
- [Architecture](#-architecture)
- [Configuration](#-configuration)
- [Database Setup](#-database-setup)
- [Running the Application](#-running-the-application)
- [Keyboard Shortcuts](#-keyboard-shortcuts)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)

---

## ✨ Features

### Core Browser Features
- 🔗 **Multi-tab browsing** with modern tab design
- 🔍 **Multiple search engines** (Google, Bing, DuckDuckGo, Yahoo, Ecosia, Brave)
- 📑 **Bookmarks** with folders and favorites
- 📜 **Browsing history** with search and filters
- ⬇️ **Download manager** with progress tracking
- 🔒 **Security indicators** (HTTPS, padlock icons)

### UI/UX Features
- 🎨 **Theme support** (Light, Dark, System)
- 🌈 **Custom accent colors**
- 🔤 **Adjustable font sizes**
- 📏 **Zoom controls** with magnifier mode
- 🖱️ **Mouse navigation** for zoomed content
- ⌨️ **Comprehensive keyboard shortcuts**

### Advanced Features
- ⚙️ **Comprehensive settings** panel
- 🛡️ **Privacy controls** (Do Not Track, block popups)
- 🧹 **Clear browsing data** on exit
- 🔧 **Developer mode**
- 💾 **Session restore**
- 🖨️ **Print support**

---

## 📸 Screenshots

*Screenshots coming soon*

---

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

### Required
- **Java Development Kit (JDK) 21** or higher
  ```bash
  # Check Java version
  java -version
  ```
  
- **Maven 3.8+** (or use the included Maven wrapper)
  ```bash
  # Check Maven version
  mvn -version
  ```

### Platform-Specific Requirements

#### Linux (Ubuntu/Debian)
```bash
# Install required libraries
sudo apt-get update
sudo apt-get install -y \
    libgtk-3-0 \
    libx11-6 \
    libxcomposite1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libdrm2 \
    libasound2 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdbus-1-3 \
    libexpat1 \
    libfontconfig1 \
    libgcc1 \
    libglib2.0-0 \
    libnspr4 \
    libnss3 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libstdc++6 \
    libxcb1 \
    libxkbcommon0 \
    libxshmfence1
```

#### Linux (Fedora)
```bash
sudo dnf install -y \
    gtk3 \
    libX11 \
    libXcomposite \
    libXdamage \
    libXext \
    libXfixes \
    libXrandr \
    mesa-libgbm \
    libdrm \
    alsa-lib \
    atk \
    at-spi2-atk \
    cups-libs \
    dbus-libs \
    expat \
    fontconfig \
    libgcc \
    glib2 \
    nspr \
    nss \
    pango \
    libstdc++ \
    libxcb \
    libxkbcommon \
    libxshmfence
```

#### Windows
- No additional dependencies required (JCEF bundles required libraries)

#### macOS
- No additional dependencies required (JCEF bundles required libraries)

---

## 🚀 Installation

### 1. Clone the Repository
```bash
git clone https:
cd nexus-browser
```

### 2. Install Dependencies
Using Maven wrapper (recommended):
```bash
# Linux/macOS
./mvnw clean install

# Windows
mvnw.cmd clean install
```

Or using system Maven:
```bash
mvn clean install
```

### 3. JCEF Native Libraries Setup

JCEF requires native Chromium libraries. On first run, jcefmaven will automatically download them. However, you may need to specify the native library path:

```bash
# Create directory for JCEF natives
mkdir -p ~/jcef-natives

# The natives will be downloaded automatically on first run
# Or you can set the path manually:
export JAVA_LIBRARY_PATH=~/jcef-natives
```

---

## 📁 Project Structure

```
nexus/
├── pom.xml                          # Maven configuration
├── mvnw                             # Maven wrapper (Linux/macOS)
├── mvnw.cmd                         # Maven wrapper (Windows)
├── browser.db                       # SQLite database (auto-created)
│
└── src/
    └── main/
        ├── java/
        │   └── com/example/nexus/
        │       │
        │       ├── core/                    # Core application classes
        │       │   ├── BrowserApplication.java    # Main application entry
        │       │   ├── DIContainer.java           # Dependency injection
        │       │   └── JCEFFactory.java           # JCEF browser factory
        │       │
        │       ├── controller/              # MVC Controllers
        │       │   ├── MainController.java        # Main window controller
        │       │   ├── TabController.java         # Tab management
        │       │   ├── BookmarkController.java    # Bookmark operations
        │       │   ├── HistoryController.java     # History operations
        │       │   ├── DownloadController.java    # Download management
        │       │   ├── SettingsController.java    # Settings management
        │       │   └── ProfileController.java     # User profiles
        │       │
        │       ├── model/                   # Data Models (POJOs)
        │       │   ├── Bookmark.java
        │       │   ├── BookmarkFolder.java
        │       │   ├── Download.java
        │       │   ├── HistoryEntry.java
        │       │   ├── Profile.java
        │       │   ├── Settings.java              # 50+ settings options
        │       │   └── Tab.java
        │       │
        │       ├── repository/              # Data Access Layer (DAO)
        │       │   ├── BaseRepository.java        # Base CRUD operations
        │       │   ├── BookmarkRepository.java
        │       │   ├── BookmarkFolderRepository.java
        │       │   ├── DownloadRepository.java
        │       │   ├── HistoryRepository.java
        │       │   ├── ProfileRepository.java
        │       │   ├── SettingsRepository.java
        │       │   └── TabRepository.java
        │       │
        │       ├── service/                 # Business Logic Layer
        │       │   ├── ISettingsService.java      # Settings interface
        │       │   ├── IBookmarkService.java      # Bookmark interface
        │       │   ├── IHistoryService.java       # History interface
        │       │   ├── SettingsService.java
        │       │   ├── BookmarkService.java
        │       │   ├── HistoryService.java
        │       │   ├── DownloadService.java
        │       │   ├── TabService.java
        │       │   ├── ProfileService.java
        │       │   └── BrowserService.java
        │       │
        │       ├── view/                    # UI Components
        │       │   ├── MainView.java              # Main window
        │       │   │
        │       │   ├── components/                # Reusable UI components
        │       │   │   ├── BrowserTab.java        # Browser tab component
        │       │   │   ├── AddressBar.java        # URL input component
        │       │   │   ├── BookmarkManager.java
        │       │   │   ├── DownloadManager.java
        │       │   │   ├── HistoryManager.java
        │       │   │   ├── ProfileSelector.java
        │       │   │   └── SettingsPanel.java
        │       │   │
        │       │   └── dialogs/                   # Dialog windows
        │       │       ├── SettingsPanel.java     # Modern settings UI
        │       │       ├── BookmarkPanel.java     # Bookmark manager
        │       │       ├── BookmarkDialog.java    # Add/edit bookmark
        │       │       ├── HistoryPanel.java      # History viewer
        │       │       ├── AboutDialog.java
        │       │       ├── FindDialog.java
        │       │       ├── PrintDialog.java
        │       │       ├── ZoomDialog.java
        │       │       ├── DevToolsDialog.java
        │       │       └── ProfileDialog.java
        │       │
        │       ├── util/                    # Utility Classes
        │       │   ├── DatabaseManager.java       # SQLite connection
        │       │   ├── ThemeManager.java          # Theme handling
        │       │   ├── IconManager.java           # Icon loading
        │       │   ├── AnimationUtils.java        # UI animations
        │       │   ├── FileUtils.java             # File operations
        │       │   ├── KeyboardShortcutManager.java
        │       │   ├── ContextMenuManager.java
        │       │   └── NotificationManager.java
        │       │
        │       ├── exception/               # Custom Exceptions
        │       │
        │       └── Launcher.java            # Application launcher
        │
        └── resources/
            └── com/example/nexus/
                ├── fxml/                    # FXML layouts
                │   ├── main.fxml                  # Main window layout
                │   ├── components/
                │   └── dialogs/
                │
                ├── css/                     # Stylesheets
                │   ├── main.css                   # Main styles
                │   ├── light.css                  # Light theme
                │   └── dark.css                   # Dark theme
                │
                ├── db/                      # Database
                │   └── init.sql                   # Schema initialization
                │
                ├── icons/                   # Application icons
                │
                ├── images/                  # Image assets
                │
                └── jcef.properties          # JCEF configuration
```

---

## 🏗️ Architecture

Nexus Browser follows a **layered architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Views     │  │   Dialogs   │  │    Components       │  │
│  │ (MainView)  │  │(SettingsPanel│  │ (BrowserTab,        │  │
│  │             │  │  BookmarkPanel│  │  AddressBar)        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     CONTROLLER LAYER                         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                  MainController                      │    │
│  │  - Handles user interactions                         │    │
│  │  - Coordinates between views and services            │    │
│  │  - Manages tab lifecycle                             │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                           │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐   │
│  │ Settings  │ │ Bookmark  │ │  History  │ │  Download │   │
│  │  Service  │ │  Service  │ │  Service  │ │  Service  │   │
│  └───────────┘ └───────────┘ └───────────┘ └───────────┘   │
│  - Business logic                                            │
│  - Data validation                                           │
│  - Change notifications                                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    REPOSITORY LAYER                          │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐   │
│  │ Settings  │ │ Bookmark  │ │  History  │ │  Download │   │
│  │   Repo    │ │   Repo    │ │   Repo    │ │   Repo    │   │
│  └───────────┘ └───────────┘ └───────────┘ └───────────┘   │
│  - CRUD operations                                           │
│  - SQL queries                                               │
│  - Data mapping                                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      DATA LAYER                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              DatabaseManager (SQLite)                │    │
│  │  - Connection pooling                                │    │
│  │  - Schema initialization                             │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Key Design Patterns

1. **Dependency Injection (DIContainer)**
   - Manages object creation and dependencies
   - Singleton instances for services

2. **Repository Pattern**
   - Abstracts data access logic
   - Clean separation from business logic

3. **Observer Pattern**
   - Settings change listeners
   - Property bindings for UI updates

4. **MVC Pattern**
   - Model: Data classes (Bookmark, Settings, etc.)
   - View: FXML layouts + JavaFX components
   - Controller: MainController

---

## ⚙️ Configuration

### JCEF Properties (`jcef.properties`)
```properties
# JCEF configuration
jcef.debug=false
jcef.remote_debugging_port=0
jcef.log_severity=disable
```

### Maven Properties (`pom.xml`)
```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <javafx.version>21.0.1</javafx.version>
</properties>
```

---

## 🗄️ Database Setup

Nexus Browser uses **SQLite** for local data storage. The database is automatically created on first run.

### Database Location
- **File**: `browser.db` (in the application root directory)
- **Auto-initialization**: Schema is created from `src/main/resources/com/example/nexus/db/init.sql`

### Database Schema

#### Users Table
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    avatar_path TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Settings Table (50+ fields)
```sql
CREATE TABLE settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    -- Appearance
    theme TEXT DEFAULT 'light',
    accent_color TEXT DEFAULT '#3b82f6',
    font_size INTEGER DEFAULT 14,
    page_zoom REAL DEFAULT 1.0,
    show_bookmarks_bar BOOLEAN DEFAULT 1,
    show_status_bar BOOLEAN DEFAULT 1,
    -- Startup & Home
    home_page TEXT DEFAULT 'https://www.google.com',
    startup_behavior TEXT DEFAULT 'show_home',
    restore_session BOOLEAN DEFAULT 1,
    -- Search
    search_engine TEXT DEFAULT 'google',
    show_search_suggestions BOOLEAN DEFAULT 1,
    -- Privacy
    clear_history_on_exit BOOLEAN DEFAULT 0,
    block_popups BOOLEAN DEFAULT 1,
    do_not_track BOOLEAN DEFAULT 1,
    save_browsing_history BOOLEAN DEFAULT 1,
    save_passwords BOOLEAN DEFAULT 1,
    -- Downloads
    download_path TEXT,
    ask_download_location BOOLEAN DEFAULT 0,
    -- Performance
    hardware_acceleration BOOLEAN DEFAULT 1,
    smooth_scrolling BOOLEAN DEFAULT 1,
    -- Advanced
    enable_javascript BOOLEAN DEFAULT 1,
    developer_mode BOOLEAN DEFAULT 0,
    -- ... more fields
    FOREIGN KEY (user_id) REFERENCES users (id)
);
```

#### Bookmarks Table
```sql
CREATE TABLE bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    favicon_url TEXT,
    folder_id INTEGER,
    position INTEGER DEFAULT 0,
    is_favorite INTEGER DEFAULT 0,
    description TEXT,
    tags TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (folder_id) REFERENCES bookmark_folders (id)
);
```

#### History Table
```sql
CREATE TABLE history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    favicon_url TEXT,
    visit_count INTEGER DEFAULT 1,
    last_visit TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id)
);
```

### Reset Database
To reset the database, simply delete `browser.db`:
```bash
rm browser.db
```
The database will be recreated on next application start.

---

## ▶️ Running the Application

### Using Maven Wrapper (Recommended)
```bash
# Linux/macOS
./mvnw javafx:run

# Windows
mvnw.cmd javafx:run
```

### Using System Maven
```bash
mvn javafx:run
```

### Using IDE (IntelliJ IDEA)

1. Open the project in IntelliJ IDEA
2. Wait for Maven to import dependencies
3. Right-click on `Launcher.java` or `BrowserApplication.java`
4. Select "Run"

**VM Options for IntelliJ:**
```
--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
--add-opens=javafx.fxml/com.sun.javafx.reflect=ALL-UNNAMED
-Djava.library.path=/path/to/jcef-natives
```

### Building Executable JAR
```bash
./mvnw clean package
java -jar target/modern-browser-1.0.0.jar
```

---

## ⌨️ Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+T` | New Tab |
| `Ctrl+W` | Close Tab |
| `Ctrl+Tab` | Next Tab |
| `Ctrl+Shift+Tab` | Previous Tab |
| `Ctrl+L` | Focus Address Bar |
| `Ctrl+R` / `F5` | Reload Page |
| `Ctrl+H` | Open History |
| `Ctrl+B` | Open Bookmarks |
| `Ctrl+D` | Bookmark Current Page |
| `Ctrl+J` | Open Downloads |
| `Ctrl++` | Zoom In |
| `Ctrl+-` | Zoom Out |
| `Ctrl+0` | Reset Zoom |
| `Alt+Z` | Toggle Magnifier Mode |
| `Alt+M` | Toggle Mouse Navigation |
| `Ctrl+F` | Find in Page |
| `Ctrl+P` | Print |
| `F12` | Developer Tools |
| `Alt+Home` | Go to Home Page |
| `Alt+Left` | Go Back |
| `Alt+Right` | Go Forward |

---

## 🔧 Troubleshooting

### Common Issues

#### 1. JCEF Native Libraries Not Found
```
Error: java.lang.UnsatisfiedLinkError: no jcef in java.library.path
```

**Solution:**
```bash
# Set library path
export JAVA_LIBRARY_PATH=~/jcef-natives

# Or add to VM options
-Djava.library.path=/path/to/jcef-natives
```

#### 2. JavaFX Runtime Components Missing
```
Error: JavaFX runtime components are missing
```

**Solution:**
Ensure you're using JDK 21 with JavaFX support or add VM options:
```
--module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml,javafx.web
```

#### 3. Database Lock Error
```
Error: database is locked
```

**Solution:**
Close any other instances of the application or database viewers.

#### 4. GTK/Display Issues on Linux
```
Error: Gdk-ERROR: The program 'java' received an X Window System error
```

**Solution:**
```bash
# Install GTK3
sudo apt-get install libgtk-3-0

# Or for Wayland issues
export GDK_BACKEND=x11
```

#### 5. Media Playback Not Working
This is a known limitation. JavaFX WebView has limited codec support. Consider using JCEF for full media support.

### Debug Mode
Enable debug logging:
```java
// In BrowserApplication.java
System.setProperty("jcef.debug", "true");
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Keep methods focused and concise

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [JCEF](https://bitbucket.org/chromiumembedded/java-cef) - Java Chromium Embedded Framework
- [JavaFX](https://openjfx.io/) - Java UI Framework
- [Ikonli](https://kordamp.org/ikonli/) - Icon Packs for Java
- [MaterialFX](https://github.com/palexdev/MaterialFX) - Material Design Components
- [AtlantaFX](https://github.com/mkpaz/atlantafx) - Modern JavaFX CSS Theme
- [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) - SQLite Database Driver

---

## 📞 Support

If you encounter any issues or have questions:
1. Check the [Troubleshooting](#-troubleshooting) section
2. Search existing [Issues](https://github.com/yourusername/nexus-browser/issues)
3. Create a new issue with detailed information

---

**Made with ❤️ using JavaFX and JCEF**

