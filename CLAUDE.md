# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Worn is a Kotlin Multiplatform wardrobe manager app for Android and iOS. Users catalog clothing items with photos (app-private storage) and get AI-powered analysis via the Anthropic Claude API (user-provided key stored in platform secret store).

## Build Commands

```shell
# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Build shared module
./gradlew :shared:compileKotlinAndroid

# Run all shared tests
./gradlew :shared:allTests

# Run a single test class
./gradlew :shared:testDebugUnitTest --tests "com.github.worn.repository.WardrobeRepositoryTest"

# Check dependency resolution
./gradlew :shared:dependencies
```

Requires JDK 17+ (SQLDelight 2.0.2 requirement). iOS builds require Xcode — open `iosApp/` directly.

## Architecture

**MVI + Repository pattern — no Use Cases layer.** Business logic lives in Repository implementations. See [ARCHITECTURE.md](./ARCHITECTURE.md) for full specification.

### Modules

- **composeApp/** — Android entry point, Compose UI (`com.github.worn`)
- **shared/** — KMP shared module (`com.github.worn.shared`)
- **iosApp/** — iOS entry point (Xcode/SwiftUI)

### Shared Module Layers

- `domain/model/` — Pure Kotlin data classes (`ClothingItem`, `Category`, `AiAnalysisResult`)
- `domain/repository/` — Repository interfaces (`WardrobeRepository`)
- `data/repository/` — Repository implementations (all business logic here)
- `data/source/local/` — SQLDelight DB + `PhotoFileStorage` (expect/actual)
- `data/source/remote/` — `ClaudeApiClient` (thin HTTP wrapper, no business logic)
- `presentation/viewmodel/` — MVI ViewModels (Intent → State + Effect, no business logic)
- `util/secret/` — `SecretStore` interface
- `di/` — Koin modules

### Platform-Specific Code

- **androidMain/** — `PhotoFileStorage` actual, `DatabaseDriverFactory` actual (AndroidSqliteDriver), `SecretStore` impl (Android Keystore)
- **iosMain/** — `PhotoFileStorage` actual, `DatabaseDriverFactory` actual (NativeSqliteDriver), `SecretStore` impl (Keychain)

### Key Patterns

- ViewModels are thin — they call the repository and map results to UI state
- `WardrobeRepositoryImpl` orchestrates AI + storage + DB in one testable place
- Photos are stored in app-private internal storage via expect/actual `PhotoFileStorage`
- SQLDelight schemas go in `shared/src/commonMain/sqldelight/`; generated DB package is `com.github.worn.data.source.local.db`

## Dependencies

Managed via `gradle/libs.versions.toml`. Key libraries: Kotlin 2.3.0, Compose Multiplatform 1.10.0, Ktor 3.1.3, SQLDelight 2.0.2, Koin 4.1.0, kotlinx-serialization 1.8.1, kotlinx-coroutines 1.10.2.

AGP 9.1.0 — shared module uses `com.android.kotlin.multiplatform.library` plugin (not the legacy `com.android.library`).

## Design

The project design file is at `design/design.pen`. Access it exclusively via the pencil MCP tools — do not use Read or Grep on `.pen` files.

## Guidelines

When adding or upgrading dependencies, always search the web for the latest stable versions of KMP-compatible libraries and follow current Kotlin Multiplatform best practices (API patterns, source set conventions, expect/actual usage). Do not assume the versions listed in `libs.versions.toml` are up to date.

This is a learning project — when explaining code or suggesting changes, always highlight relevant KMP best practices (e.g., expect/actual patterns, source set hierarchy, shared vs platform-specific code decisions, multiplatform testing strategies).

After completing all changes in a plan, always run `./gradlew detekt` and fix any reported issues before considering the work done.

Reference these for version compatibility and best practices:
- https://developer.android.com/kotlin/multiplatform
- https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html#kotlin-2-0-0-and-later
