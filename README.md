# 🌐 Nexus Browser

> A desktop web browser built with JavaFX and JCEF, combining a Chromium-based browsing engine with a structured Java application architecture.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-2C2255?style=flat-square)](https://openjfx.io/)
[![JCEF](https://img.shields.io/badge/JCEF-Chromium-4285F4?style=flat-square&logo=googlechrome&logoColor=white)](https://bitbucket.org/chromiumembedded/java-cef)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![SQLite](https://img.shields.io/badge/SQLite-Local%20Storage-003B57?style=flat-square&logo=sqlite&logoColor=white)](https://www.sqlite.org/)

## Overview

Nexus Browser is a modern desktop browser application built with **Java 21**, **JavaFX**, and **JCEF (Java Chromium Embedded Framework)**.

The project goes beyond embedding a web page in a desktop window: it implements browser-oriented features such as tabs, history, bookmarks, downloads, profiles, settings, themes, session restoration, privacy controls, and developer tooling while keeping UI, business logic, and persistence separated.

## Highlights

### Browsing
- Multi-tab browsing
- Back / forward / refresh navigation
- Multiple search engines
- HTTPS/security indicators
- Session restoration
- Print support

### Productivity
- Bookmarks and bookmark folders
- Searchable browsing history
- Download manager with progress tracking
- User profiles
- Comprehensive keyboard shortcuts

### Customization & Privacy
- Light / dark / system themes
- Custom accent colors
- Adjustable font sizes
- Page zoom controls
- Do Not Track and popup controls
- Clear browsing data
- Developer mode

## Architecture

Nexus follows a layered architecture with explicit separation of responsibilities:

```text
┌──────────────────────────────────────────────┐
│                Presentation                  │
│      Views · Components · Dialogs           │
└──────────────────────┬───────────────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│                 Controllers                  │
│       User interaction & UI coordination     │
└──────────────────────┬───────────────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│                   Services                   │
│    Browser · Tabs · History · Bookmarks      │
│    Downloads · Profiles · Settings           │
└──────────────────────┬───────────────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│                 Repositories                 │
│              Data access / CRUD              │
└──────────────────────┬───────────────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│                  SQLite DB                   │
└──────────────────────────────────────────────┘
```

### Design patterns
- **MVC** — separates models, views, and controllers.
- **Repository Pattern** — isolates persistence and SQL operations.
- **Dependency Injection** — centralizes object creation through `DIContainer`.
- **Observer Pattern** — propagates settings and UI state changes.

## Technology Stack

| Technology | Role |
|---|---|
| Java 21 | Application language/runtime |
| JavaFX 21 | Desktop UI |
| JCEF | Chromium-based browser engine |
| Maven | Build and dependency management |
| SQLite | Local persistence |
| FXML / CSS | UI structure and styling |

## Project Structure

```text
src/main/java/com/example/nexus/
├── core/             # Application bootstrap, DI and JCEF
├── controller/       # UI/application controllers
├── model/            # Domain models
├── repository/       # Persistence layer
├── service/          # Business logic
├── view/             # JavaFX UI components and dialogs
├── util/             # Database, themes, icons, shortcuts, etc.
└── exception/        # Application exceptions

src/main/resources/com/example/nexus/
├── fxml/             # UI layouts
├── css/              # Light/dark themes
├── db/               # SQLite initialization
├── icons/
└── images/
```

The application models browser concerns as dedicated components, including bookmarks, history, downloads, profiles, tabs, and settings. The settings model contains **50+ configuration options** covering appearance, startup behavior, browsing preferences, and UI behavior.

## Database

Nexus uses SQLite for local application data. The schema is initialized automatically from the project's SQL resources.

Core persisted concepts include:
- Users / profiles
- Browser settings
- Tabs / session state
- Bookmarks and folders
- History entries
- Downloads

## Getting Started

### Prerequisites
- JDK 21+
- Maven 3.8+ or the included Maven wrapper
- Platform dependencies required by JCEF

### Build

```bash
./mvnw clean install
```

Windows:

```powershell
mvnw.cmd clean install
```

### Run

```bash
./mvnw javafx:run
```

If your platform requires an explicit native library path for JCEF, configure it before launching the application.

## Platform Notes

JCEF uses native Chromium libraries, so Linux systems may require additional desktop libraries such as GTK, X11, GBM, NSS, and related runtime dependencies.

Windows and macOS use the native libraries bundled/downloaded through the JCEF setup.

## Screenshots

Screenshots can be added here to showcase the browser UI, settings, bookmarks, downloads, and dark/light themes.

## Project Status

Nexus Browser is an actively developed engineering project focused on desktop application architecture, embedded Chromium integration, local persistence, UI state management, and browser feature implementation.
