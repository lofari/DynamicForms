# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

All Gradle commands require JDK 17 (system defaults to JVM 8):

```bash
# Android
cd android && JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew assembleDebug

# Backend
cd backend && JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew run
```

### Running Tests

```bash
# All Android unit tests
cd android && JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew test

# Single module tests
cd android && JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew :core:domain:test
cd android && JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew :feature:form-wizard:test

# Single test class
cd android && JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew :core:domain:test --tests "com.lfr.dynamicforms.domain.usecase.ValidatePageUseCaseTest"

# Static analysis
cd android && JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew detekt

# Android instrumentation tests (requires emulator/device)
cd android && JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew connectedAndroidTest

# Maestro E2E tests (requires running app on emulator)
maestro test android/.maestro/

# Backend tests
cd backend && JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew test
```

## Architecture

MVI + Clean Architecture with Hilt DI, organized into a multi-module structure.

### Module Structure

```
android/
  build-logic/                        # Convention plugins (shared build config)
  core/model/                         # Pure Kotlin: domain models (FormElement, Form, Page, etc.)
  core/domain/                        # Pure Kotlin: use cases + repository interfaces
  core/data/                          # Android lib: Room, Retrofit, repos, WorkManager
  core/ui/                            # Android lib: theme, shared composables, common strings
  core/testing/                       # Pure Kotlin: MainDispatcherRule, test helpers
  feature/form-list/                  # Android feature: FormListScreen + ViewModel
  feature/form-wizard/                # Android feature: FormScreen + ViewModel + elements
  app/                                # Thin shell: DI wiring, navigation, Application class
backend/                              # Standalone Ktor server (Kotlin)
admin/                                # React admin panel (TypeScript, Vite)
```

### Convention Plugins (android/build-logic/)

| Plugin ID | Purpose |
|-----------|---------|
| `dynamicforms.kotlin.library` | Pure Kotlin modules (JVM 11, serialization) |
| `dynamicforms.android.library` | Android library modules (SDK config) |
| `dynamicforms.android.application` | App module |
| `dynamicforms.android.feature` | Feature modules (android.library + compose + hilt + navigation) |
| `dynamicforms.compose` | Compose BOM + deps |
| `dynamicforms.hilt` | Hilt + KSP |
| `dynamicforms.detekt` | Static analysis |

### Key Patterns

- **Polymorphic JSON**: `FormElement` subtypes are deserialized via `kotlinx.serialization` with `classDiscriminator = "type"`. The Json instance is configured in `NetworkModule`.
- **Conditional visibility**: Elements have optional `visibleWhen: VisibilityCondition` evaluated at runtime. Hidden fields are excluded from validation and submission.
- **Repeating groups**: `RepeatingGroupElement` nests child elements. Field IDs use indexed format: `contacts[0].name`.
- **Draft persistence**: Auto-saved to Room on page navigation and field changes. Resumed from form list.
- **FormEffect channel**: One-shot side effects (navigation, errors) are emitted via `Channel` and collected as `Flow` in composables.
- **Structured logging**: Timber with DebugTree (debug) and ReleaseTree (release).

## Version Compatibility

These constraints are hard-won and must be respected when updating dependencies:

- AGP 9.0 requires Hilt >= 2.59.1 (older versions fail with "BaseExtension not found")
- Room >= 2.7.0 required with KSP 2.0.21 (2.6.x fails with "unexpected jvm signature V")
- KSP must be 2.0.21-1.0.26 (1.0.28 is incompatible)
- `android.disallowKotlinSourceSets=false` in gradle.properties is required for KSP + AGP 9.0
- AGP 9.0 uses `com.android.build.api.dsl.LibraryExtension` (not `com.android.build.gradle.LibraryExtension`)

## Testing

- **Unit tests**: MockK for mocking, Turbine for Flow assertions, `MainDispatcherRule` (`:core:testing`) for coroutine testing.
  - `:core:model` — FormElement default values, JSON deserialization
  - `:core:domain` — Use case logic (7 use cases + visibility evaluation)
  - `:core:data` — Repository implementations
  - `:feature:form-wizard` — FormViewModel, FormUiState
  - `:feature:form-list` — FormListViewModel
- **Compose UI tests**: Compose testing framework with semantic matchers.
  - `:feature:form-wizard` — Element composables, FormScreen, FormSuccessScreen
  - `:feature:form-list` — FormListScreen
- **Maestro E2E** (`android/.maestro/`): YAML flows testing full user journeys. Tests reference `testTag` values (`field_*`, `btn_*`).
