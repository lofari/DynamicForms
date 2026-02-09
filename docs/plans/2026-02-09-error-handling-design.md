# Error Handling Consistency & Architecture

**Date:** 2026-02-09
**Goal:** Unify error handling across all layers with a single result type, structured errors, and consistent propagation.

## Problem

Error handling uses mixed patterns: `DomainResult` in one use case, a custom `SubmitResult` sealed class in another, raw exceptions in the rest. Repositories throw raw exceptions. Some errors surface to the UI, others are silently swallowed. The worker never reports failure to WorkManager.

## Design

### Error Model (`core/model`)

A sealed class representing all error categories:

```kotlin
sealed class DomainError {
    data class Network(val cause: Throwable? = null) : DomainError()
    data class Timeout(val cause: Throwable? = null) : DomainError()
    data object NotFound : DomainError()
    data class Server(val code: Int? = null, val message: String? = null) : DomainError()
    data class Validation(val fieldErrors: Map<String, String>) : DomainError()
    data class Storage(val cause: Throwable? = null) : DomainError()
    data class Unknown(val cause: Throwable? = null) : DomainError()
}
```

`DomainResult.Failure` holds a `DomainError` instead of a raw `Throwable`:

```kotlin
sealed interface DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>
}
```

New extensions for composing results:

```kotlin
fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R>
suspend fun <T, R> DomainResult<T>.flatMap(transform: suspend (T) -> DomainResult<R>): DomainResult<R>
```

`SubmitResult` is deleted. `SubmitFormUseCase` returns `DomainResult<SubmissionResponse>` instead.

### Repository Boundary (`core/data`)

A shared `safeCall` helper catches exceptions and maps them to `DomainError`:

```kotlin
suspend fun <T> safeCall(block: suspend () -> T): DomainResult<T> =
    try {
        DomainResult.Success(block())
    } catch (e: UnknownHostException) {
        DomainResult.Failure(DomainError.Network(e))
    } catch (e: SocketTimeoutException) {
        DomainResult.Failure(DomainError.Timeout(e))
    } catch (e: HttpException) {
        when (e.code()) {
            404 -> DomainResult.Failure(DomainError.NotFound)
            else -> DomainResult.Failure(DomainError.Server(e.code(), e.message()))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        DomainResult.Failure(DomainError.Unknown(e))
    }
```

Logs each failure at creation: `Timber.e(cause, "DomainError: ${error::class.simpleName}")`.

Per repository:

- **FormRepositoryImpl** — wraps `getForm()` and `submitForm()` in `safeCall`. Returns `DomainResult<Form>` and `DomainResult<SubmissionResponse>`.
- **DraftRepositoryImpl** — wraps Room calls in `safeCall`. Corrupted-draft fallback stays (returns `Success(null)` after deleting bad data). Other Room failures become `DomainError.Storage`.
- **SubmissionQueueRepositoryImpl** — wraps Room/JSON operations in `safeCall`. Existing graceful fallbacks for deserialization stay inside the `safeCall` block.

Repository interfaces in `core/domain` update their return types to `DomainResult<T>`.

### Use Cases (`core/domain`)

Use cases drop try-catch and compose results with `fold()`/`map()`/`flatMap()`:

- **GetFormUseCase** — `formRepository.getForm(id).map { mergeDraftValues(it, draft) }`.
- **SubmitFormUseCase** — returns `DomainResult<SubmissionResponse>`. Client-side validation failure returns `Failure(DomainError.Validation(fieldErrors))`. Server call delegates to repo.
- **SyncSubmissionUseCase** — replaces try-catch with `fold()` on repo result. Retry counting and status updates unchanged.
- **SaveDraftUseCase** — returns `DomainResult<Unit>`.
- **EnqueueSubmissionUseCase** — returns `DomainResult<Unit>`.
- **ValidatePageUseCase** — no change (no I/O, returns `Map<String, String>`).

### Presentation (`core/ui`, `feature/*`)

**ErrorMapping** accepts `DomainError` instead of `Throwable`:

```kotlin
fun DomainError.toUserMessage(): String = when (this) {
    is DomainError.Network -> "No internet connection. Check your network and try again."
    is DomainError.Timeout -> "Request timed out. Please try again."
    is DomainError.NotFound -> "Form not found."
    is DomainError.Server -> "Server error. Please try again later."
    is DomainError.Validation -> "Please fix the errors below."
    is DomainError.Storage -> "Could not save data. Please try again."
    is DomainError.Unknown -> "Something went wrong. Please try again."
}
```

**FormViewModel** — uses `fold()` on all results. Submission checks `response.fieldErrors` on success; maps `DomainError.Validation` to field errors, other errors to `FormEffect.ShowError`. Draft save stays silent (Timber warning only).

**FormListViewModel** — uses `fold()` instead of try-catch. `retrySubmission()`/`discardSubmission()` surface errors via a new `FormListEffect.ShowError`.

**New `FormListEffect`:**

```kotlin
sealed interface FormListEffect {
    data class ShowError(val message: String) : FormListEffect
}
```

### Worker (`core/data`)

`SyncSubmissionsWorker` inspects `DomainResult` from `SyncSubmissionUseCase`:
- All submissions processed (success or permanently failed) -> `Result.success()`
- Any transient error (Network/Timeout) -> `Result.retry()`

### Out of Scope

- Crash reporting (Crashlytics/Sentry)
- OkHttp timeouts and retry interceptor
- Exponential backoff in SyncSubmissionUseCase
- Structured error codes beyond DomainError variants

## Changes by Module

| Module | Changes |
|--------|---------|
| `core/model` | Add `DomainError` sealed class, update `DomainResult.Failure`, add `map()`/`flatMap()` extensions, delete `SubmitResult` |
| `core/domain` | Repository interfaces return `DomainResult<T>`, use cases drop try-catch and use `fold()`/`map()` |
| `core/data` | Add `safeCall` helper, all repo impls return `DomainResult<T>`, worker uses `Result.retry()` |
| `core/ui` | `ErrorMapping` accepts `DomainError` instead of `Throwable` |
| `feature/form-wizard` | `FormViewModel` uses `fold()` on all results, submission logic simplified |
| `feature/form-list` | `FormListViewModel` uses `fold()`, add `FormListEffect` |
| Tests | Update all tests to assert `DomainResult`/`DomainError` instead of raw exceptions |
