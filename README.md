# DynamicForms

An Android app that receives polymorphic JSON form definitions from a backend, parses them by element type, and renders dynamic multi-step form wizards. Includes a Ktor backend server and a React admin panel for managing form definitions and viewing submissions.

## Features

- **Dynamic form rendering from JSON** -- 14 element types parsed via polymorphic `kotlinx.serialization`
- **Multi-page wizard** with animated page transitions and progress bar tracking
- **Client-side validation** with real-time inline error feedback (required, regex, min/max, length)
- **Conditional field visibility** -- 7 operators (`equals`, `not_equals`, `greater_than`, `less_than`, `contains`, `is_empty`, `is_not_empty`)
- **Repeating groups** with dynamic add/remove rows (indexed field IDs: `contacts[0].name`)
- **Auto-saving drafts** to Room DB on every page navigation and field change, with one-tap resume from the form list
- **Offline submission queue** -- submissions enqueued locally and synced via WorkManager when connectivity returns
- **Skeleton loading** with shimmer animation during form fetch
- **Accessibility** -- heading semantics, live regions for error announcements, content descriptions on all interactive elements
- **Italian localization** (English + Italian)
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

# Build Android app
cd android && ./gradlew assembleDebug

# Run unit tests
cd android && ./gradlew test

# Static analysis
cd android && ./gradlew detekt

# Start the Ktor backend
cd backend && ./gradlew run
```

## Testing

| Layer | Location | What is covered |
|-------|----------|-----------------|
| Unit tests | `android/core/model`, `android/core/domain`, `android/core/data`, `android/feature/form-wizard`, `android/feature/form-list` | Domain use cases, repository implementations, ViewModel logic, JSON deserialization, UI state |
| Compose UI tests | `android/feature/form-wizard`, `android/feature/form-list` | One test file per form element + screen-level tests for form, list, and success screens |
| Migration tests | `android/core/data` (androidTest) | Room schema migration v1 to v2 preserves drafts and creates pending_submissions table |
| Maestro E2E | `android/.maestro/` | Happy-path submissions, validation errors, page navigation, form list browsing |
| Backend tests | `backend/` | Ktor route tests for form, admin, and submission endpoints |

```bash
# All Android unit tests
cd android && ./gradlew test

# Single module tests
cd android && ./gradlew :core:domain:test
cd android && ./gradlew :feature:form-wizard:test

# Compose UI tests (requires emulator)
cd android && ./gradlew connectedAndroidTest

# Maestro E2E (requires running app on emulator)
maestro test android/.maestro/

# Backend tests
cd backend && ./gradlew test
```

---

## Architecture

```
DynamicForms/
├── android/               Android Gradle project
│   ├── build-logic/       Gradle convention plugins (shared build config)
│   ├── core/
│   │   ├── model/         Pure Kotlin: domain models (FormElement, Form, Page, etc.)
│   │   ├── domain/        Pure Kotlin: use cases + repository interfaces
│   │   ├── data/          Android lib: Room, Retrofit, repositories, WorkManager
│   │   ├── ui/            Android lib: theme, shared composables, common strings
│   │   └── testing/       Pure Kotlin: MainDispatcherRule, test helpers
│   ├── feature/
│   │   ├── form-wizard/   Android feature: FormScreen + ViewModel + elements
│   │   └── form-list/     Android feature: FormListScreen + ViewModel
│   └── app/               Thin shell: DI wiring, navigation, Application class
├── backend/               Standalone Ktor API server (Kotlin)
├── admin/                 React admin panel (TypeScript, Vite)
└── docs/                  Design documents and plans
```

### Android App

MVI + Clean Architecture with Hilt dependency injection, organized as a multi-module Gradle project.

**Convention Plugins** (`android/build-logic/`) provide consistent build configuration across all modules: SDK versions, Compose setup, Hilt/KSP wiring, and detekt static analysis.

| Module | Contents |
|--------|----------|
| `:core:model` | `FormElement` sealed class (14 subtypes), `Form`, `Page`, validation models |
| `:core:domain` | `GetFormUseCase`, `ValidatePageUseCase`, `EvaluateVisibilityUseCase`, `SubmitFormUseCase`, `SaveDraftUseCase`, `EnqueueSubmissionUseCase`, `SyncSubmissionUseCase` |
| `:core:data` | Retrofit API, `MockInterceptor`, Room database (drafts + pending submissions), `SyncSubmissionsWorker` |
| `:core:ui` | Material3 theme, shared composables (`ErrorText`, `ShimmerSkeleton`), `ErrorMapping` |
| `:feature:form-wizard` | MVI components (`FormViewModel`, `FormUiState`, `FormAction`, `FormEffect`), one composable per element type, animated page transitions, skeleton loading, success screen with bounce animation |
| `:feature:form-list` | Form list with draft resume indicator, pending submissions section |
| `:app` | `DynamicFormsApp`, `MainActivity`, navigation graph, Hilt DI modules (`NetworkModule`, `DatabaseModule`, `RepositoryModule`) |

### Backend (Ktor)

Standalone Kotlin server providing REST APIs for form definitions and submissions.

- **Routes**: `GET /forms`, `GET /forms/{id}`, `POST /forms/{id}/submit`
- **Admin routes**: `POST /admin/forms`, `PUT /admin/forms/{id}`, `DELETE /admin/forms/{id}`, `GET /admin/forms/{id}/submissions`
- **Storage**: SQLite via Exposed with seed form templates loaded from JSON resources
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
cd backend && ./gradlew run   # http://localhost:8080
```

**Build the admin panel** (requires Node.js 18+):

```bash
cd admin && npm install && npm run build   # outputs to backend/src/main/resources/admin/
```

Visit `http://localhost:8080/admin/` once the backend is running. For dev with hot reload: `cd admin && npm run dev`.

**Connect the Android app:** The debug build auto-connects to `http://10.0.2.2:8080` (emulator alias for localhost). The release build uses `MockInterceptor` for offline operation, controlled by `BuildConfig.USE_MOCK` in `android/app/build.gradle.kts`.

</details>

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Android UI | Jetpack Compose + Material3 |
| Architecture | MVI + Clean Architecture, multi-module Gradle |
| Build | Convention plugins (`android/build-logic/`), R8 shrinking, detekt static analysis |
| DI | Hilt |
| Serialization | kotlinx.serialization (polymorphic, `classDiscriminator = "type"`) |
| Networking | Retrofit + OkHttp |
| Local storage | Room (with tested schema migrations) |
| Background work | WorkManager (offline submission sync) |
| Logging | Timber |
| Navigation | Navigation Compose (type-safe routes) |
| Accessibility | Heading semantics, live regions, content descriptions |
| Localization | English, Italian |
| Backend | Ktor 3.1.1 + Netty |
| Admin panel | React 19, TypeScript, Vite, Tailwind CSS, Monaco Editor |
| Testing | JUnit, MockK, Turbine, Compose UI tests, Ktor test host, Maestro E2E |
