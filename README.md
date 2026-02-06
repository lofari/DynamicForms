# DynamicForms

An Android app that receives polymorphic JSON form definitions from a backend, parses them by element type, and renders dynamic multi-step form wizards. Includes a Ktor backend server and a React admin panel for managing form definitions and viewing submissions.

## Features

- **Dynamic form rendering from JSON** -- 14 element types parsed via polymorphic `kotlinx.serialization`
- **Multi-page wizard** with progress bar tracking and page-level validation
- **Client-side validation** with real-time inline error feedback (required, regex, min/max, length)
- **Conditional field visibility** -- 7 operators (`equals`, `not_equals`, `greater_than`, `less_than`, `contains`, `is_empty`, `is_not_empty`)
- **Repeating groups** with dynamic add/remove rows (indexed field IDs: `contacts[0].name`)
- **Auto-saving drafts** to Room DB on every page navigation and field change, with one-tap resume from the form list
- **Signature capture**, file upload placeholder, and Material3 date picker
- **Real backend** (Ktor) + **React admin panel** with Monaco JSON editor, live preview, and CSV export of submissions

## Demo

https://github.com/user-attachments/assets/demo-placeholder

> Replace the link above with a screen recording or GIF showing a form being filled, validated, and submitted.

## Running the App

**Prerequisites:** Android Studio, JDK 17, Android emulator or device (API 24+).

```bash
# Set JAVA_HOME (required -- system JDK defaults to 8)
export JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home

# Build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Optional: start the Ktor backend (the debug build auto-connects via 10.0.2.2)
./gradlew -PincludeBackend=true :backend:run
```

## Testing

| Layer | Files | What is covered |
|-------|------:|-----------------|
| Unit tests (`app/src/test/`) | 13 | Domain use cases, repository implementations, ViewModel logic, JSON deserialization, UI state |
| Compose UI tests (`app/src/androidTest/`) | 17 | One test file per form element + screen-level tests for form, list, and success screens |
| Maestro E2E (`.maestro/`) | 4 flows | Happy-path submissions, validation errors, page navigation, form list browsing |

```bash
# All unit tests
JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew test

# Compose UI tests (requires emulator)
JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew connectedAndroidTest

# Maestro E2E (requires running app on emulator)
maestro test .maestro/
```

---

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

## Setup Details

<details>
<summary>Backend and admin panel setup (optional)</summary>

**Start the backend:**

```bash
./gradlew -PincludeBackend=true :backend:run   # http://localhost:8080
```

**Build the admin panel** (requires Node.js 18+):

```bash
cd admin && npm install && npm run build   # outputs to backend/src/main/resources/admin/
```

Visit `http://localhost:8080/admin/` once the backend is running. For dev with hot reload: `cd admin && npm run dev`.

**Connect the Android app:** The debug build auto-connects to `http://10.0.2.2:8080` (emulator alias for localhost). The release build uses `MockInterceptor` for offline operation, controlled by `BuildConfig.USE_MOCK` in `app/build.gradle.kts`.

**Note:** The backend module is excluded from Android Studio indexing by default. Include it with `-PincludeBackend=true`.

</details>

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
