# DynamicForms

An Android app that receives polymorphic JSON form definitions from a backend, parses them by element type, and renders dynamic multi-step form wizards. Includes a Ktor backend server and a React admin panel for managing form definitions and viewing submissions.

## Architecture

```
DynamicForms/
├── app/            Android app (Kotlin, Jetpack Compose)
├── backend/        Ktor API server (Kotlin)
└── admin/          React admin panel (TypeScript, Vite)
```

### Android App

MVI + Clean Architecture with Hilt dependency injection.

| Layer | Contents |
|-------|----------|
| `domain/model/` | `FormElement` sealed class (14 subtypes), `Form`, `Page`, validation models |
| `domain/usecase/` | `GetFormUseCase`, `ValidatePageUseCase`, `EvaluateVisibilityUseCase`, `SubmitFormUseCase`, `SaveDraftUseCase` |
| `domain/repository/` | `FormRepository`, `DraftRepository` interfaces |
| `data/remote/` | Retrofit API + `MockInterceptor` for offline development |
| `data/local/` | Room database for draft persistence |
| `data/repository/` | Repository implementations |
| `di/` | Hilt modules: `NetworkModule`, `DatabaseModule`, `RepositoryModule` |
| `presentation/elements/` | One composable per element type (text field, dropdown, slider, etc.) |
| `presentation/form/` | MVI components: `FormViewModel`, `FormUiState`, `FormAction`, `FormEffect` |
| `presentation/list/` | Form list screen with draft resume indicator |
| `presentation/navigation/` | Type-safe Navigation Compose routes |

### Backend (Ktor)

Kotlin server providing REST APIs for form definitions and submissions.

- **Routes**: `GET /forms`, `GET /forms/{id}`, `POST /forms/{id}/submit`
- **Admin routes**: `POST /admin/forms`, `PUT /admin/forms/{id}`, `DELETE /admin/forms/{id}`, `GET /admin/forms/{id}/submissions`
- **Storage**: In-memory with 5 seed form templates loaded from JSON resources
- **Validation**: Server-side validation mirroring the Android app's logic (visibility-aware)

### Admin Panel (React)

Single-page application served by Ktor at `/admin`.

- **Dashboard**: Card grid of all forms with edit, submissions, and delete actions
- **Form Editor**: Split-pane with Monaco JSON editor and live form preview
- **Submissions Viewer**: Dynamic table with expandable rows and CSV export

## Supported Form Elements

| Type | Description |
|------|-------------|
| `text_field` | Single or multiline text input with regex/length validation |
| `number_field` | Numeric input with min/max validation |
| `dropdown` | Single-select dropdown menu |
| `radio` | Radio button group |
| `checkbox` | Single checkbox with default value |
| `toggle` | Boolean toggle switch |
| `multi_select` | Multi-option selection with min/max count validation |
| `slider` | Range slider with configurable min, max, and step |
| `date_picker` | Date selection with optional min/max dates |
| `file_upload` | File attachment with allowed types and size limit |
| `signature` | Signature capture pad |
| `repeating_group` | Nested group of elements that can be duplicated |
| `section_header` | Non-input section divider with optional subtitle |
| `label` | Static display text |

All elements support conditional visibility via `visibleWhen`, which evaluates field values against operators like `equals`, `not_equals`, `greater_than`, `less_than`, `contains`, `is_empty`, and `is_not_empty`.

## Prerequisites

- **JDK 17** (the project uses Gradle 9.1 which requires JDK 17+)
- **Android Studio** (for the Android app)
- **Node.js 18+** (for the admin panel)

## Getting Started

### 1. Set JAVA_HOME

The system JDK must be version 17. All Gradle commands require:

```bash
export JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home
```

### 2. Build the Android App

```bash
./gradlew assembleDebug
```

### 3. Start the Backend

```bash
./gradlew -PincludeBackend=true :backend:run
```

The server starts on `http://localhost:8080`. Verify with:

```bash
curl http://localhost:8080/forms
```

### 4. Build the Admin Panel

```bash
cd admin
npm install
npm run build    # outputs to backend/src/main/resources/admin/
```

Once built and the backend is running, visit `http://localhost:8080/admin/`.

For development with hot reload:

```bash
cd admin
npm run dev      # proxies API calls to localhost:8080
```

### 5. Connect the Android App to the Backend

The debug build is configured to connect to the real backend at `http://10.0.2.2:8080` (the emulator's alias for host localhost). Start the backend first, then run the app on an emulator.

The release build uses the `MockInterceptor` for offline operation. This is controlled by `BuildConfig.USE_MOCK` and `BuildConfig.BASE_URL` in `app/build.gradle.kts`.

## Running Tests

```bash
# Android unit tests
./gradlew testDebugUnitTest

# Android instrumented tests (requires emulator)
./gradlew connectedAndroidTest

# Backend tests
./gradlew -PincludeBackend=true :backend:test

# Maestro E2E tests (requires running app on emulator)
maestro test .maestro/
```

## Project Configuration

The backend module is excluded from Android Studio by default to avoid indexing issues. It is only included when the `includeBackend` Gradle property is set:

```bash
./gradlew -PincludeBackend=true :backend:build
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Android UI | Jetpack Compose + Material3 |
| Architecture | MVI + Clean Architecture |
| DI | Hilt |
| Serialization | kotlinx.serialization (polymorphic, `classDiscriminator = "type"`) |
| Networking | Retrofit + OkHttp |
| Local storage | Room |
| Navigation | Navigation Compose (type-safe routes) |
| Backend | Ktor 3.1.1 + Netty |
| Admin panel | React 19, TypeScript, Vite, Tailwind CSS, Monaco Editor |
| Testing | JUnit, MockK, Turbine, Compose UI tests, Ktor test host |
