# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

All Gradle commands require JDK 17 (system defaults to JVM 8):

```bash
JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew assembleDebug
```

### Running Tests

```bash
# All unit tests
JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew test

# Single test class
JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew testDebugUnitTest --tests "com.lfr.dynamicforms.domain.usecase.ValidatePageUseCaseTest"

# Android instrumentation tests (requires emulator/device)
JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew connectedAndroidTest

# Maestro E2E tests (requires running app on emulator)
maestro test .maestro/
```

## Architecture

MVI + Clean Architecture with Hilt DI. Single-module app (`com.lfr.dynamicforms`).

### Layer Structure

- **`domain/model/`** - Pure Kotlin models. `FormElement` is a sealed class with 14 subtypes, discriminated by `@SerialName` on `"type"` for polymorphic JSON deserialization. `Form` contains `Page`s, each containing `FormElement`s.
- **`domain/usecase/`** - Business logic: `GetFormUseCase`, `ValidatePageUseCase`, `EvaluateVisibilityUseCase`, `SubmitFormUseCase`, `SaveDraftUseCase`.
- **`domain/repository/`** - Repository interfaces (`FormRepository`, `DraftRepository`).
- **`data/remote/`** - Retrofit API with `MockInterceptor` providing hardcoded JSON responses. Base URL: `https://api.dynamicforms.mock/`.
- **`data/local/`** - Room database (`AppDatabase`, `DraftDao`, `DraftEntity`). Drafts are stored as serialized JSON values.
- **`data/repository/`** - Repository implementations.
- **`di/`** - Three Hilt modules: `NetworkModule` (Json, OkHttp, Retrofit, API), `DatabaseModule` (Room), `RepositoryModule` (bindings).
- **`presentation/form/`** - MVI components: `FormAction` (sealed intents), `FormUiState` (persistent state), `FormEffect` (one-shot events via Channel), `FormViewModel`.
- **`presentation/elements/`** - One composable per `FormElement` type (e.g., `DynamicTextField`, `DynamicDropdown`). All use `testTag("field_{id}")` for UI testing.
- **`presentation/list/`** - Form list screen with draft resume indicator.
- **`presentation/navigation/`** - Type-safe Navigation Compose routes using `@Serializable` data objects/classes.

### Key Patterns

- **Polymorphic JSON**: `FormElement` subtypes are deserialized via `kotlinx.serialization` with `classDiscriminator = "type"`. The Json instance is configured in `NetworkModule`.
- **Conditional visibility**: Elements have optional `visibleWhen: VisibilityCondition` evaluated at runtime. Hidden fields are excluded from validation and submission.
- **Repeating groups**: `RepeatingGroupElement` nests child elements. Field IDs use indexed format: `contacts[0].name`.
- **Draft persistence**: Auto-saved to Room on page navigation and field changes. Resumed from form list.
- **FormEffect channel**: One-shot side effects (navigation, errors) are emitted via `Channel` and collected as `Flow` in composables, preventing duplicate processing on recomposition.

## Version Compatibility

These constraints are hard-won and must be respected when updating dependencies:

- AGP 9.0 requires Hilt >= 2.59.1 (older versions fail with "BaseExtension not found")
- Room >= 2.7.0 required with KSP 2.0.21 (2.6.x fails with "unexpected jvm signature V")
- KSP must be 2.0.21-1.0.26 (1.0.28 is incompatible)
- `android.disallowKotlinSourceSets=false` in gradle.properties is required for KSP + AGP 9.0

## Testing

- **Unit tests** (`app/src/test/`): MockK for mocking, Turbine for Flow assertions, `MainDispatcherRule` for coroutine testing.
- **Compose UI tests** (`app/src/androidTest/`): Compose testing framework with semantic matchers.
- **Maestro E2E** (`.maestro/`): YAML flows testing full user journeys. Tests reference `testTag` values (`field_*`, `btn_*`).
