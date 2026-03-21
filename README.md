# Worn

A wardrobe manager for Android and iOS, built with Kotlin Multiplatform. Catalog your clothing with photos and let AI auto-tag and categorize items using the Claude API.

## Features

- Catalog clothing items with photos stored in app-private storage
- AI-powered image analysis and auto-tagging (via Anthropic Claude API)
- Search and filter by category, color, or tags
- Cross-platform: Android and iOS from a single codebase

## Architecture

MVI + Repository pattern with no Use Cases layer. Business logic lives in Repository implementations. See [ARCHITECTURE.md](./ARCHITECTURE.md) for full details.

## Tech Stack

| Library | Purpose |
|---|---|
| Kotlin Multiplatform | Shared business logic (Android + iOS) |
| Compose Multiplatform | Shared UI (Android) |
| kotlinx-coroutines | Async / Flow |
| Ktor | HTTP client (Claude API) |
| kotlinx-serialization | JSON |
| SQLDelight | Local database |
| Koin | Dependency injection |

## Project Structure

```
root/
├── composeApp/     # Android app entry point + Compose UI
├── iosApp/         # iOS app entry point (Xcode/SwiftUI)
└── shared/         # KMP shared module
    ├── commonMain/ # Domain, data, and presentation layers
    ├── androidMain/# Android platform implementations
    └── iosMain/    # iOS platform implementations
```

## Build & Run

### Android

```shell
./gradlew :composeApp:assembleDebug
```

Or use the run configuration in Android Studio / Fleet.

### iOS

Open `iosApp/` in Xcode and run, or use the KMP run configuration in Fleet.
