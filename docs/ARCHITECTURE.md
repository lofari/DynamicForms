# Architecture Decision Record

**Project:** DynamicForms -- an Android app that parses polymorphic JSON into dynamic multi-step form wizards.

**Stack:** Kotlin, Jetpack Compose, Hilt, Room, Retrofit, kotlinx.serialization.

---

## 1. MVI over MVVM

**Decision:** Adopt Model-View-Intent with a single `FormUiState` data class, a `FormAction` sealed class for intents, and a `FormEffect` channel for one-shot events.

**Why:** Dynamic forms have complex state interactions -- field values, validation errors, visibility toggles, page navigation, repeating group counts, and submission status all change in response to user input. MVI forces all mutations through a single `onAction(FormAction)` entry point, making state transitions explicit and traceable.

The `FormUiState` data class holds the entire screen state:

```kotlin
data class FormUiState(
    val isLoading: Boolean = true,
    val form: Form? = null,
    val currentPageIndex: Int = 0,
    val values: Map<String, String> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
    val repeatingGroupCounts: Map<String, Int> = emptyMap(),
    val visibleElementIds: Set<String> = emptySet(),
    val isSubmitting: Boolean = false
)
```

**Trade-offs:** More boilerplate than MVVM -- every user interaction requires a `FormAction` subclass and a corresponding handler in the ViewModel. In practice, the 8 action types (`LoadForm`, `UpdateField`, `NextPage`, `PrevPage`, `Submit`, `SaveDraft`, `AddRepeatingRow`, `RemoveRepeatingRow`) are manageable, and the payoff is that any state can be reproduced by replaying actions.

---

## 2. Channel for Side Effects over SharedFlow

**Decision:** Use `Channel<FormEffect>(Channel.BUFFERED)` exposed as a `Flow` via `receiveAsFlow()` for navigation and error events.

**Why:** Side effects like `NavigateToSuccess` and `ShowError` must be processed exactly once. `SharedFlow` with `replay = 0` drops events if no collector is active at emission time (e.g., during configuration changes). `Channel` buffers events and delivers each to exactly one collector.

```kotlin
private val _effect = Channel<FormEffect>(Channel.BUFFERED)
val effect = _effect.receiveAsFlow()
```

**Trade-offs:** Channel supports only a single collector. This is fine for our use case -- one composable screen collects effects -- but would not work if multiple observers needed the same event. If that requirement arose, `SharedFlow` with `replay = 1` and manual consumption tracking would be the alternative.

---

## 3. Polymorphic JSON with kotlinx.serialization

**Decision:** Model form elements as a `FormElement` sealed class with 14 subtypes, deserialized polymorphically using `classDiscriminator = "type"`.

**Why:** The JSON API uses a `"type"` field to distinguish element kinds (`"text_field"`, `"dropdown"`, `"repeating_group"`, etc.). Rather than writing a manual `when` block to parse each type, we configure the `Json` instance with `classDiscriminator = "type"` and annotate each subtype with `@SerialName`:

```kotlin
@Serializable sealed class FormElement { ... }

@Serializable @SerialName("text_field")
data class TextFieldElement(...) : FormElement()

@Serializable @SerialName("dropdown")
data class DropdownElement(...) : FormElement()
```

Adding a new element type requires only a new data class with `@SerialName` -- no changes to parsing logic, no factory method updates.

**Trade-offs:** Every subtype must carry the `@SerialName` annotation or deserialization silently fails. The sealed class hierarchy also means all 14 types live in a single file (`FormElement.kt`), which is large but keeps the type system self-documenting. Recursive nesting (e.g., `RepeatingGroupElement` containing `List<FormElement>`) works automatically with this approach.

---

## 4. Clean Architecture Layers

**Decision:** Three-layer structure with strict dependency direction: `domain` -> `data` -> `presentation`.

| Layer | Contents | Android Dependencies |
|-------|----------|---------------------|
| `domain/model/` | `FormElement`, `Form`, `Page`, `VisibilityCondition` | None (pure Kotlin) |
| `domain/usecase/` | `GetFormUseCase`, `ValidatePageUseCase`, `EvaluateVisibilityUseCase`, `SubmitFormUseCase`, `SaveDraftUseCase` | None |
| `domain/repository/` | `FormRepository`, `DraftRepository` interfaces | None |
| `data/remote/` | `FormApi` (Retrofit), `MockInterceptor` | OkHttp |
| `data/local/` | `AppDatabase`, `DraftDao`, `DraftEntity` | Room |
| `data/repository/` | Repository implementations | Android (Room, Retrofit) |
| `presentation/` | Compose UI, ViewModels, navigation | Full Android/Compose |

**Why:** The domain layer contains business rules (validation, visibility evaluation) that can be unit-tested without Android framework dependencies. Use cases are injected into ViewModels, and repository interfaces are defined in domain so the data layer can be swapped without touching business logic.

**Trade-offs:** More files and indirection than a flat package structure. For a single-module demo app, this is arguably over-engineered. The benefit is that each layer has clear responsibilities, and tests at each level verify different concerns without mocking the world.

---

## 5. Hilt for Dependency Injection

**Decision:** Use Hilt with three modules: `NetworkModule`, `DatabaseModule`, `RepositoryModule`.

**Why:** Hilt provides compile-time dependency graph validation, constructor injection for ViewModels via `@HiltViewModel`, and scoping to Android lifecycle components. The three modules map to the three infrastructure concerns:

- **NetworkModule** -- `Json` instance (with `classDiscriminator`), `OkHttpClient` (with conditional `MockInterceptor`), `Retrofit`, `FormApi`
- **DatabaseModule** -- Room `AppDatabase`, `DraftDao`
- **RepositoryModule** -- Binds `FormRepository` and `DraftRepository` interfaces to implementations

**Trade-offs:** Hilt adds annotation processing overhead (~2-3s to incremental builds) and requires the `@AndroidEntryPoint` annotation on activities/fragments. For this project size, Koin or manual DI would also work. Hilt was chosen because it is the officially recommended DI solution for Android and demonstrates familiarity with the standard toolchain.

---

## 6. Draft Persistence with Room

**Decision:** Auto-save form state to Room on field changes (debounced 2 seconds) and page navigation. Store draft values as a serialized JSON blob.

**Why:** Users expect to resume partially completed forms. The ViewModel schedules saves via a coroutine debounce pattern:

```kotlin
private fun scheduleDraftSave() {
    draftDebounceJob?.cancel()
    draftDebounceJob = viewModelScope.launch {
        delay(2000L)
        saveDraftNow()
    }
}
```

Drafts are also saved immediately on page transitions and when the ViewModel is cleared. The `DraftEntity` stores the form ID, current page index, and a JSON string of all field values.

**Trade-offs:** Storing values as a JSON blob means you cannot query individual field values in SQL. This is acceptable because drafts are always loaded and saved as a unit -- there is no use case for querying "all drafts where field X equals Y". The upside is that adding new form fields never requires a Room schema migration.

---

## 7. Conditional Visibility

**Decision:** Elements declare an optional `visibleWhen` condition evaluated against current field values at runtime. Hidden elements are excluded from both validation and submission.

**Why:** Real-world forms frequently show/hide fields based on other answers (e.g., show "Company Name" only when role is "Developer", show hazard details only when hazards are found). The `VisibilityCondition` model supports 7 operators:

| Operator | Example Use |
|----------|-------------|
| `equals` | Show field when dropdown value matches |
| `not_equals` | Show field for all values except one |
| `greater_than` | Show field when numeric value exceeds threshold |
| `less_than` | Show field when numeric value is below threshold |
| `contains` | Show field when text contains substring |
| `is_empty` | Show field when referenced field has no value |
| `is_not_empty` | Show field when referenced field has any value |

Visibility is recalculated on every field change via `EvaluateVisibilityUseCase`, and the resulting `visibleElementIds` set drives both rendering and validation filtering.

**Trade-offs:** Currently supports only single-condition visibility (one `visibleWhen` per element). Compound conditions (AND/OR) would require a condition tree structure. The single-condition model covers the majority of real-world form logic and keeps the evaluation code straightforward.

---

## 8. Three-Layer Testing Strategy

**Decision:** Unit tests, Compose UI tests, and Maestro E2E tests, each targeting different concerns.

| Layer | Framework | What It Tests |
|-------|-----------|---------------|
| Unit | JUnit + MockK + Turbine | Use cases, ViewModel state transitions, validation logic |
| UI | Compose Testing + semantic matchers | Individual composable rendering, field interactions |
| E2E | Maestro (YAML flows) | Full user journeys across screens |

**Why:** Unit tests verify business logic in isolation (e.g., `ValidatePageUseCase` returns correct errors for each field type). Compose UI tests verify that composables render correctly given a state and emit correct actions on interaction. Maestro tests verify end-to-end flows (fill form, navigate pages, submit) on a real device/emulator.

All form element composables use `testTag("field_{id}")` for reliable selection in both Compose tests and Maestro flows.

**Trade-offs:** Three test layers means more test infrastructure to maintain. Maestro tests are slow (~30s per flow) and require a running emulator. The benefit is confidence at each abstraction level -- a unit test failure pinpoints the broken logic, while a Maestro failure catches integration issues that unit tests miss.

---

## 9. Mock Interceptor for Offline Development

**Decision:** An OkHttp `MockInterceptor` returns hardcoded JSON responses when `BuildConfig.USE_MOCK` is true. Debug builds can connect to a real Ktor backend.

**Why:** The mock interceptor enables fully offline development and deterministic UI testing. It intercepts requests by path pattern and returns pre-built JSON:

```kotlin
val (code, json) = when {
    method == "GET" && path == "/forms" -> 200 to FORM_LIST_JSON
    method == "GET" && path.matches(Regex("/forms/[^/]+")) -> 200 to getFormJson(formId)
    method == "POST" && path.matches(Regex("/forms/[^/]+/submit")) -> 200 to SUBMIT_SUCCESS_JSON
    else -> 404 to """{"error":"not found"}"""
}
```

The interceptor is conditionally added in `NetworkModule` based on the build config flag, so production builds never include mock data in the HTTP pipeline.

**Trade-offs:** Mock data can drift from the real API contract over time. The 5 hardcoded form templates (registration, feedback, safety inspection, job application, event registration) exercise all 14 element types and most visibility conditions, which is sufficient for demo purposes. A shared JSON schema or contract test would be the next step for a production system.
