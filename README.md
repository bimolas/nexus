# Nexus Browser

> A desktop web browser built with Java 21, JavaFX, JCEF and SQLite, designed as a practical exercise in desktop software architecture and native browser integration.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-2C2255?style=flat-square)](https://openjfx.io/)
[![JCEF](https://img.shields.io/badge/JCEF-Chromium-4285F4?style=flat-square&logo=googlechrome&logoColor=white)](https://bitbucket.org/chromiumembedded/java-cef/)
[![SQLite](https://img.shields.io/badge/SQLite-local%20database-003B57?style=flat-square&logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![Maven](https://img.shields.io/badge/Maven-build-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)

## Why this project?

Nexus Browser explores what it takes to build a real desktop application rather than a simple UI prototype. The project combines JavaFX for the application interface with JCEF for Chromium-based web rendering and SQLite for persistent local browser data.

The codebase is organized around controllers, services, repositories, models and reusable UI components, with explicit dependency management through a small DI container.

## Features

### Browsing

- Multi-tab browsing
- Chromium-based rendering through JCEF
- Multiple search engines
- Back / forward / refresh / home navigation
- Address bar and browser controls
- HTTPS/security indicators
- Zoom controls

### Personal data

- Bookmarks and bookmark folders
- Searchable browsing history
- Download manager with progress tracking
- Browser profiles
- Session restoration
- SQLite persistence

### Customization

- Light, dark and system themes
- Accent colors
- Font-size controls
- Configurable browser settings
- Keyboard shortcuts
- Developer mode
- Privacy-related controls

## Architecture

```text
┌──────────────────────────────────────────┐
│              Presentation                │
│   Views · Components · Dialogs · FXML    │
└────────────────────┬─────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────┐
│              Controllers                 │
│     User interaction + orchestration     │
└────────────────────┬─────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────┐
│                Services                  │
│  Browser · Tabs · Bookmarks · History    │
│  Downloads · Profiles · Settings         │
└────────────────────┬─────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────┐
│              Repositories                │
│          CRUD + persistence logic        │
└────────────────────┬─────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────┐
│            SQLite / local data           │
└──────────────────────────────────────────┘

                 JCEF / Chromium
                        │
                        ▼
                 Web page rendering
```

### Design patterns used

- **MVC** — separates JavaFX views, controllers and application data.
- **Repository Pattern** — isolates persistence and SQL access from business logic.
- **Dependency Injection** — `DIContainer` manages application dependencies and service instances.
- **Observer / Property Binding** — propagates settings and UI state changes.

## Tech Stack

| Area | Technologies |
|---|---|
| Language | Java 21 |
| Desktop UI | JavaFX 21 |
| Browser engine | JCEF / Chromium |
| Persistence | SQLite + JDBC |
| Build | Maven |
| UI libraries | MaterialFX, AtlantaFX, Ikonli |
| Architecture | MVC, layered architecture, Repository, DI, Observer |

## Project Structure

```text
src/main/java/com/example/nexus/
├── core/            # Application bootstrap, DI and JCEF integration
├── controller/      # JavaFX controllers
├── model/           # Domain models
├── repository/      # SQLite data access
├── service/         # Business logic
├── view/            # UI components and dialogs
├── util/            # Database, themes, shortcuts and utilities
└── exception/       # Application-specific exceptions

src/main/resources/
├── fxml/            # Layouts
├── css/             # Theme stylesheets
├── db/              # Database initialization
├── icons/            # Icons
└── images/           # Application assets
```

## Getting Started

### Requirements

- JDK 21+
- Maven 3.8+ or the included Maven wrapper
- A platform supported by the JCEF native binaries

### Clone

```bash
git clone https://github.com/bimolas/nexus.git
cd nexus
```

### Build

Linux / macOS:

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

If your environment requires explicit JCEF native configuration, follow the platform-specific setup documented in the project configuration.

## Data

Nexus uses SQLite for local browser state. The database is initialized automatically and stores application data such as bookmarks, history, downloads, profiles, tabs and settings.

## Screenshots

Screenshots can be added here as the UI evolves. The repository is intentionally structured so the README can showcase the application without mixing presentation assets into the Java source tree.

## What this project demonstrates

- Building a non-trivial Java desktop application
- Integrating a native Chromium browser engine into JavaFX
- Designing layered application architecture
- Separating UI, business logic and persistence
- Managing persistent local application state
- Building reusable desktop UI components
- Working with Maven and third-party Java libraries

## Status

Nexus Browser is an ongoing personal project. The focus is on improving the browser experience while keeping the codebase maintainable and modular.
