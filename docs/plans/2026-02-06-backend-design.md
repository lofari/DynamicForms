# Backend Design: Ktor API + React Admin Panel

## Purpose

Replace the Android app's MockInterceptor with a real backend. Serve form definitions from JSON files, accept submissions, and provide a polished admin panel for managing forms. The goal is a portfolio-quality full-stack Kotlin project.

## Architecture

A single Ktor application serves both the REST API (consumed by the Android app) and the admin panel (a React SPA served as static files). Forms are stored as JSON files on disk; submissions are held in memory.

```
Android App  ──>  Ktor API  ──>  FormStore (JSON files)
                    │                SubmissionStore (in-memory)
Admin SPA    ──>  Admin API ──/
```

## API Endpoints

### Mobile API (matches existing MockInterceptor contract)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/forms` | List form summaries (id, title, description, pageCount, fieldCount) |
| GET | `/forms/{formId}` | Full form definition with pages and elements |
| POST | `/forms/{formId}/submit` | Submit `{ formId, values }`, returns `{ success, message, fieldErrors }` |

### Admin API

| Method | Path | Description |
|--------|------|-------------|
| POST | `/admin/forms` | Create a new form definition |
| PUT | `/admin/forms/{formId}` | Update an existing form definition |
| DELETE | `/admin/forms/{formId}` | Delete a form and its submissions |
| GET | `/admin/forms/{formId}/submissions` | List submissions for a form |

## Data Model

### Form Definition (JSON files)

Same polymorphic JSON structure the Android app expects. 14 element types: `text_field`, `number_field`, `dropdown`, `radio`, `checkbox`, `toggle`, `multi_select`, `slider`, `date_picker`, `file_upload`, `signature`, `repeating_group`, `section_header`, `label`.

Each element supports `visibleWhen` conditions with operators: `equals`, `not_equals`, `greater_than`, `less_than`, `contains`, `is_empty`, `is_not_empty`.

### Submission

```kotlin
data class Submission(
    val id: String,          // UUID
    val formId: String,
    val values: Map<String, String>,
    val submittedAt: Instant
)
```

Stored in a `ConcurrentHashMap<String, List<Submission>>` keyed by formId. Lost on restart (acceptable for a showcase).

### Storage

- **FormStore**: Loads JSON files from `resources/forms/` at startup into an in-memory map. Admin CRUD operations write back to disk and update the cache.
- **SubmissionStore**: In-memory only. Provides submission counts for the dashboard.

## Server-Side Validation

`Validator.kt` mirrors the Android client's `ValidatePageUseCase`:

- Required field checks (visibility-aware: skips hidden fields)
- Text: minLength, maxLength, regex pattern
- Number: min, max, numeric format
- Selection: minSelections, maxSelections

`VisibilityEvaluator.kt` mirrors `EvaluateVisibilityUseCase` to determine which fields are visible given current values.

On failure, the response returns `success: false` with a `fieldErrors` map. The Android app already handles this in `SubmitResult.ServerError`.

## Admin Panel

### Tech Stack

- React + TypeScript + Vite
- Tailwind CSS
- Monaco Editor (JSON editing with schema validation)
- Built output served by Ktor at `/admin`

### Views

**Dashboard (`/admin`)** — Card grid of forms showing title, description, field/page/submission counts. Actions: edit, duplicate, delete. "Create New Form" button.

**Form Editor (`/admin/forms/{formId}/edit`)** — Split-pane layout. Left: Monaco JSON editor with real-time schema validation and error markers. Right: live preview rendering form pages as styled cards (section headers, field labels, input placeholders, page navigation). Top bar: save, title, back.

**Submissions Viewer (`/admin/forms/{formId}/submissions`)** — Table with one row per submission, auto-detected columns from form fields, click to expand details, CSV export.

### Visual Design

Dark sidebar navigation, light content area. Accent color matching the Android app's Material3 primary. Clean typography via Tailwind defaults.

## Project Structure

```
backend/
  build.gradle.kts
  src/main/kotlin/com/lfr/dynamicforms/
    Application.kt                    # Entry point, plugins, static file serving
    routes/
      FormRoutes.kt                   # Mobile API endpoints
      AdminRoutes.kt                  # Admin CRUD + submissions
    model/
      FormDefinition.kt              # Form/Page/Element data classes
      Submission.kt                   # Submission data class
    validation/
      Validator.kt                    # Server-side field validation
      VisibilityEvaluator.kt         # visibleWhen condition evaluation
    storage/
      FormStore.kt                   # JSON file load/save + in-memory cache
      SubmissionStore.kt             # ConcurrentHashMap storage
  src/main/resources/
    forms/                            # Seed JSON files (5 forms)
    admin/                            # Built React SPA
  src/test/kotlin/
    routes/FormRoutesTest.kt
    routes/AdminRoutesTest.kt
    validation/ValidatorTest.kt

admin/
  package.json
  vite.config.ts                      # Outputs to ../backend/src/main/resources/admin/
  src/
    App.tsx
    pages/Dashboard.tsx
    pages/FormEditor.tsx
    pages/Submissions.tsx
    components/FormPreview.tsx
```

## Testing

- **Ktor routes**: `testApplication {}` for each endpoint (no server startup)
- **Validator**: Unit tests for each element type and visibility evaluation
- **Admin**: Component tests with Vitest

## Running Locally

```bash
cd admin && npm install && npm run build   # Build SPA into backend resources
cd backend && ./gradlew run                # Start Ktor on port 8080
```

- API available at `http://localhost:8080/forms`
- Admin panel at `http://localhost:8080/admin`

## Android App Changes

- Update `NetworkModule.kt` to point base URL at `http://10.0.2.2:8080` (emulator localhost)
- Remove or keep MockInterceptor behind a build flavor flag

## Seed Data

The 5 existing mock forms ship as JSON files:
1. `registration_v1.json` — Multi-page user registration
2. `feedback_v1.json` — Simple feedback form
3. `safety_inspection_v1.json` — Safety checklist with repeating groups
4. `job_application_v1.json` — Job application with work history
5. `event_registration_v1.json` — Event registration with conditional fields
