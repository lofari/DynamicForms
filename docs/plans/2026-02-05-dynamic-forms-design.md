# DynamicForms -- Design Document

## Overview

Android app that receives polymorphic JSON form definitions from a backend, parses them by element type, and renders dynamic multi-step form wizards. Forms support validation, conditional visibility, repeating groups, draft auto-save, and server submission.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **Architecture:** MVI + Clean Architecture
- **DI:** Hilt
- **Serialization:** Kotlin Serialization (polymorphic via `classDiscriminator = "type"`)
- **Networking:** Retrofit with mock interceptor
- **Local storage:** Room (draft persistence)
- **Navigation:** Navigation Compose with type-safe Kotlin Serialization routes

## JSON Structure

```json
{
  "formId": "registration_v1",
  "title": "User Registration",
  "pages": [
    {
      "pageId": "page_1",
      "title": "Personal Info",
      "elements": [
        {
          "id": "full_name",
          "type": "text_field",
          "label": "Full Name",
          "required": true,
          "visibleWhen": null,
          "validation": {
            "minLength": 2,
            "maxLength": 100,
            "pattern": null,
            "errorMessage": "Name must be 2-100 characters"
          }
        },
        {
          "id": "age",
          "type": "number_field",
          "label": "Age",
          "required": true,
          "validation": { "min": 18, "max": 120 }
        },
        {
          "id": "newsletter",
          "type": "toggle",
          "label": "Subscribe to newsletter",
          "defaultValue": true
        },
        {
          "id": "role",
          "type": "dropdown",
          "label": "Role",
          "required": true,
          "options": [
            { "value": "dev", "label": "Developer" },
            { "value": "design", "label": "Designer" }
          ]
        },
        {
          "id": "company_name",
          "type": "text_field",
          "label": "Company Name",
          "visibleWhen": { "fieldId": "role", "operator": "equals", "value": "dev" }
        }
      ]
    }
  ]
}
```

### Key JSON conventions

- Polymorphic dispatch on the `type` field per element.
- Each element carries `id`, `label`, `required`, `visibleWhen`, and type-specific fields.
- Pages define wizard steps, each with a title and ordered element list.
- Conditional visibility via `visibleWhen` with operator-based conditions.
- Repeating groups are an element type containing nested `elements`.

## Architecture

### Package Structure

```
com.lfr.dynamicforms/
├── domain/
│   ├── model/          # Form, Page, FormElement sealed hierarchy, ValidationRule, FormState
│   ├── repository/     # FormRepository, DraftRepository (interfaces)
│   └── usecase/        # GetFormUseCase, SubmitFormUseCase, SaveDraftUseCase, etc.
│
├── data/
│   ├── remote/         # FormApi (Retrofit), DTOs with @Serializable
│   ├── local/          # RoomDatabase, DraftDao, DraftEntity
│   ├── mapper/         # DTO <-> Domain mappers
│   └── repository/     # FormRepositoryImpl, DraftRepositoryImpl
│
├── di/                 # Hilt modules (NetworkModule, DatabaseModule, RepositoryModule)
│
├── presentation/
│   ├── list/           # FormListViewModel, FormListScreen (home screen)
│   ├── form/           # FormViewModel, FormScreen, FormAction, FormState, FormEffect
│   ├── elements/       # One Composable per element type
│   └── navigation/     # NavHost, type-safe route definitions
│
└── ui/theme/           # Material3 theme (existing)
```

### MVI Flow

- **FormAction** (sealed class): `LoadForm`, `UpdateField`, `NextPage`, `PrevPage`, `Submit`, `SaveDraft`
- **FormState** (data class): current page index, form definition, field values map, validation errors map, loading/error/success states
- **FormEffect** (sealed class): `NavigateToSuccess`, `ShowError`, `DraftSaved` -- one-shot side effects via Channel

FormState is persistent, re-readable UI state. FormEffect is consumed exactly once (navigation, snackbars). Separation prevents duplicate side effects on configuration changes.

### Key Boundaries

- Domain knows nothing about Retrofit, Room, or Compose.
- Repository interfaces live in domain; implementations in data.
- ViewModels depend only on UseCases.
- UseCases depend only on repository interfaces.

## Element Type Hierarchy

Sealed class hierarchy with Kotlin Serialization polymorphic dispatch:

| Type              | SerialName         | Type-specific fields                          |
|-------------------|--------------------|-----------------------------------------------|
| TextFieldElement  | `text_field`       | multiline, validation (minLength, maxLength, pattern) |
| NumberFieldElement| `number_field`     | validation (min, max)                         |
| DropdownElement   | `dropdown`         | options (value/label pairs)                   |
| CheckboxElement   | `checkbox`         | defaultValue                                  |
| RadioElement      | `radio`            | options (value/label pairs)                   |
| DatePickerElement | `date_picker`      | minDate, maxDate, format                      |
| ToggleElement     | `toggle`           | defaultValue                                  |
| SliderElement     | `slider`           | min, max, step                                |
| MultiSelectElement| `multi_select`     | options, validation (minSelections, maxSelections) |
| FileUploadElement | `file_upload`      | maxFileSize, allowedTypes                     |
| SignatureElement  | `signature`        | (none beyond base)                            |
| SectionHeaderElement | `section_header`| title, subtitle                               |
| LabelElement      | `label`            | text                                          |
| RepeatingGroupElement | `repeating_group` | minItems, maxItems, elements (nested)      |

All elements share: `id`, `label`, `required`, `visibleWhen`.

### Rendering

A single `FormElementRenderer` composable dispatches via `when` on the sealed type to per-element composables.

## UseCases

| UseCase               | Responsibility                                                     |
|------------------------|--------------------------------------------------------------------|
| GetFormUseCase         | Fetch form definition, check for existing draft, merge saved values |
| SubmitFormUseCase      | Client-side validation across all pages, POST to backend, delete draft on success |
| SaveDraftUseCase       | Persist current page index + field values to Room                  |
| ValidatePageUseCase    | Validate all visible fields on a given page, return error map      |
| EvaluateVisibilityUseCase | Given current field values, compute which elements are visible  |

## Conditional Visibility

```kotlin
@Serializable
data class VisibilityCondition(
    val fieldId: String,
    val operator: ConditionOperator,
    val value: JsonPrimitive
)

enum class ConditionOperator {
    EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, CONTAINS, IS_EMPTY, IS_NOT_EMPTY
}
```

- Evaluated in `EvaluateVisibilityUseCase` on every field value change.
- Hidden fields are excluded from validation and submission.
- Inside repeating groups, `fieldId` resolves to the current row first, then global scope.

## Repeating Groups

- Rendered with "Add" button (up to `maxItems`) and "Remove" per row.
- Field values keyed with index: `contacts[0].name`, `contacts[1].name`.
- Validation runs per-row.
- Removing a row shifts subsequent values down.

## Draft Saving

### Storage

```
Room table: drafts
- formId: String (PK)
- pageIndex: Int
- valuesJson: String (serialized field values map)
- updatedAt: Long
```

### Auto-save triggers

- On `NextPage` and `PrevPage` actions.
- On `onStop` lifecycle event.
- Debounced (2-3 seconds) on field value changes.

### Draft resume

- Form list screen shows a "resume" indicator for forms with saved drafts.
- Opening a form with a draft restores page index and field values.

## Form Submission

1. User taps "Submit" on the last page.
2. Client-side validation runs on all pages (not just current).
3. If validation fails, navigate to first page with errors, highlight fields.
4. If valid, POST answers to mock API.
5. Server responds with success or per-field errors.
6. On server errors, map them to the validation errors map, navigate to relevant page.
7. On success, delete local draft, emit `NavigateToSuccess` effect.

### Submission payload

```json
{
  "formId": "registration_v1",
  "values": {
    "full_name": "Lorenzo",
    "age": 28,
    "newsletter": true,
    "contacts[0].name": "Alice",
    "contacts[0].email": "alice@example.com"
  }
}
```

## Networking

- Retrofit interface with `@GET` for form list and form detail, `@POST` for submission.
- Mock interceptor returns hardcoded JSON responses.
- Kotlin Serialization converter for request/response parsing.

## Navigation

Type-safe Navigation Compose with `@Serializable` route objects:

- `FormListRoute` -- home screen, list of available forms
- `FormWizardRoute(formId: String)` -- multi-step form wizard
- `FormSuccessRoute(formId: String)` -- submission success screen

## Validation

### Client-side (backend-defined rules)

- `TextValidation`: minLength, maxLength, pattern (regex), errorMessage
- `NumberValidation`: min, max, errorMessage
- `SelectionValidation`: minSelections, maxSelections (for multi_select)
- `required` field checked for all element types

### Server-side

- Server validates on submit and returns per-field errors.
- App parses server error response and maps errors to the corresponding fields.
