# LogcatFilter

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-1.9.21-blue.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose%20Desktop-1.5.11-green.svg" alt="Compose Desktop">
  <img src="https://img.shields.io/badge/Platform-macOS%20%7C%20Windows-lightgrey.svg" alt="Platform">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
</p>

Real-time Android ADB Logcat Viewer & Filter Tool

## 📖 Introduction

LogcatFilter is a desktop application for Android developers. It monitors ADB logcat output in real-time and provides **powerful query-based filtering like Android Studio**.

## ✨ Features

### 🔴 Real-time Logcat Streaming
- Display logs from connected devices/emulators via ADB in real-time
- Start/Stop capture control

### 📦 Automatic Package Name Display
- Automatically maps PID to package name (like Android Studio)

### 🔍 Android Studio Style Filtering
```
package:com.example          # Package name filter
tag:MainActivity             # Tag filter
level:warn                   # WARN level and above only
-package:excluded            # Exclusion filter
package~:com\.example\..*    # Regex support
package:foo package:bar      # OR operation (foo or bar)
```

### 🔎 Search (Separate from Filter)
- Dedicated search bar for text search within logs
- Search result count (1/101)
- Navigate to previous/next results with ↑↓
- Search term highlighting

### 💾 Log Export/Import
- Export logs to file
- Import saved log files
- Compatible with `adb logcat >> file` format

### 🎨 UI Features
- Color-coded log levels (V/D/I/W/E/F)
- Drag to resize columns
- Show/hide column settings
- Auto-scroll FAB toggle
- Status bar (total logs, filtered count, capture status)

## ⌨️ Keyboard Shortcuts

| Function | macOS | Windows/Linux |
|----------|-------|---------------|
| Open Search | `⌘ + F` | `Ctrl + F` |
| Next Result | `⌘ + F` (when search open) | `Ctrl + F` |
| Previous Result | `⌘ + Shift + F` | `Ctrl + Shift + F` |
| Next Result | `Enter` (in search bar) | `Enter` |
| Previous Result | `Shift + Enter` (in search bar) | `Shift + Enter` |
| Close Search | `Escape` | `Escape` |

## 📋 Filter Syntax

| Keyword | Description | Example |
|---------|-------------|---------|
| `package:` | Package name contains | `package:com.example` |
| `package=:` | Package name exact match | `package=:com.example.app` |
| `package~:` | Package name regex | `package~:com\.example\..*` |
| `-package:` | Exclude package | `-package:test` |
| `tag:` | Tag filter | `tag:MainActivity` |
| `level:` | Level and above (v/d/i/w/e/f) | `level:warn` |
| `level=:` | Exact level match | `level=:error` |
| `message:` | Message search | `message:exception` |
| `pid:` / `tid:` | PID/TID filter | `pid:1234` |

**Aliases**: `pkg:`, `msg:`, `lvl:`, `process:`

**Combination Rules**:
- Multiple same keys → **OR** (`package:foo package:bar`)
- Different keys → **AND** (`package:foo tag:Main`)

## 🛠️ Installation & Running

### Requirements
- JDK 17 or higher
- Android SDK Platform Tools (ADB)
- ADB must be registered in system PATH

### Run
```bash
# Run from source
./gradlew run

# Or download release package
# (Download from Releases page)
```

### Build
```bash
# Run tests
./gradlew test

# Create macOS package
./gradlew packageDmg

# Create Windows package
./gradlew packageMsi
```

## 📸 Screenshot

```
┌──────────────────────────────────────────────────────────────────┐
│ [Device: ▼ Pixel 6 Pro]  [▶ Start] [⏹ Stop] [🗑 Clear] [📥][📤]   │
├──────────────────────────────────────────────────────────────────┤
│ 🔍 [package:com.example tag:Main level:warn____________] [x]     │
├──────────────────────────────────────────────────────────────────┤
│ Time          │ Package / PID    │ TID  │ Lvl │ Tag     │ Message│
├───────────────┼──────────────────┼──────┼─────┼─────────┼────────┤
│ 01-12 14:23:45│ com.example.app  │ 1234 │  D  │ Main    │ start  │
│ 01-12 14:23:46│ com.example.app  │ 1235 │  I  │ Life    │ resume │
│ 01-12 14:23:47│ com.google.gms   │ 5678 │  W  │ GMS     │ warn   │
│                                                          [↓] FAB │
├──────────────────────────────────────────────────────────────────┤
│ Total: 1,234  │  Filtered: 456  │  ● Capturing                   │
└──────────────────────────────────────────────────────────────────┘
```

## 🔧 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Compose Desktop (Jetpack Compose for Desktop)
- **Build Tool**: Gradle (Kotlin DSL)
- **Test Framework**: JUnit5 + kotlinx-coroutines-test

## 📁 Project Structure

```
logcatFilter/
├── src/
│   ├── main/kotlin/
│   │   ├── Main.kt
│   │   ├── ui/
│   │   │   ├── MainWindow.kt
│   │   │   ├── viewmodel/MainViewModel.kt
│   │   │   ├── components/
│   │   │   │   ├── ToolBar.kt
│   │   │   │   ├── FilterPanel.kt
│   │   │   │   ├── LogTable.kt
│   │   │   │   ├── SearchBar.kt
│   │   │   │   └── StatusBar.kt
│   │   │   └── theme/Theme.kt
│   │   ├── domain/model/
│   │   │   ├── LogEntry.kt
│   │   │   ├── LogLevel.kt
│   │   │   ├── FilterQuery.kt
│   │   │   └── ColumnConfig.kt
│   │   └── data/
│   │       ├── adb/AdbService.kt
│   │       ├── parser/LogParser.kt
│   │       ├── repository/LogRepository.kt
│   │       └── file/LogFileManager.kt
│   └── test/kotlin/
├── build.gradle.kts
└── README.md
```

## 🤝 Contributing

Bug reports, feature suggestions, and PRs are all welcome!

## 📜 License

MIT License

## 🙏 Acknowledgments

- [iookill/LogFilter](https://github.com/iookill/LogFilter) - Project that inspired this
