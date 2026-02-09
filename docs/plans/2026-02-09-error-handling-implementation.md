# Error Handling Consistency Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Unify error handling across all layers with a single `DomainResult` type, structured `DomainError` sealed class, and consistent propagation from repositories through use cases to ViewModels.

**Architecture:** Repositories catch exceptions via a shared `safeCall` helper and return `DomainResult<T>`. Use cases compose results with `map()`/`flatMap()`/`fold()` — no try-catch. ViewModels map `DomainError` to user messages via `ErrorMapping`. `SubmitResult` and `SyncResult` are deleted. Worker uses `Result.retry()` for transient errors.

**Tech Stack:** Kotlin sealed classes, kotlinx.coroutines, MockK, Turbine, Retrofit HttpException, Room

---

### Task 1: Add DomainError sealed class and update DomainResult

**Files:**
- Modify: `core/model/src/main/kotlin/com/lfr/dynamicforms/domain/model/DomainResult.kt`

**Step 1: Update DomainResult.kt with DomainError and new extensions**

Replace the entire file:

```kotlin
package com.lfr.dynamicforms.domain.model

/**
 * Structured error types for domain operations.
 */
sealed class DomainError {
    data class Network(val cause: Throwable? = null) : DomainError()
    data class Timeout(val cause: Throwable? = null) : DomainError()
    data object NotFound : DomainError()
    data class Server(val code: Int? = null, val message: String? = null) : DomainError()
    data class Validation(val fieldErrors: Map<String, String>) : DomainError()
    data class Storage(val cause: Throwable? = null) : DomainError()
    data class Unknown(val cause: Throwable? = null) : DomainError()
}

/**
 * A typed result wrapper for domain operations, replacing raw exception propagation
 * with explicit success/failure in the type signature.
 */
sealed interface DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>
}

inline fun <T, R> DomainResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (DomainError) -> R
): R = when (this) {
    is DomainResult.Success -> onSuccess(data)
    is DomainResult.Failure -> onFailure(error)
}

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> =
    when (this) {
        is DomainResult.Success -> DomainResult.Success(transform(data))
        is DomainResult.Failure -> this
    }

suspend inline fun <T, R> DomainResult<T>.flatMap(
    crossinline transform: suspend (T) -> DomainResult<R>
): DomainResult<R> =
    when (this) {
        is DomainResult.Success -> transform(data)
        is DomainResult.Failure -> this
    }
```

**Step 2: Verify core:model compiles**

Run: `JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew :core:model:build`
Expected: BUILD SUCCESSFUL (tests in core:model don't touch DomainResult)

**Step 3: Commit**

```bash
git add core/model/src/main/kotlin/com/lfr/dynamicforms/domain/model/DomainResult.kt
git commit -m "feat: add DomainError sealed class and update DomainResult"
```

---

### Task 2: Add safeCall helper in core:data

**Files:**
- Create: `core/data/src/main/java/com/lfr/dynamicforms/data/repository/SafeCall.kt`

**Step 1: Create SafeCall.kt**

```kotlin
package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

private const val HTTP_NOT_FOUND = 404

suspend inline fun <T> safeCall(crossinline block: suspend () -> T): DomainResult<T> =
    try {
        DomainResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: UnknownHostException) {
        Timber.e(e, "DomainError: Network")
        DomainResult.Failure(DomainError.Network(e))
    } catch (e: SocketTimeoutException) {
        Timber.e(e, "DomainError: Timeout")
        DomainResult.Failure(DomainError.Timeout(e))
    } catch (e: HttpException) {
        val error = when (e.code()) {
            HTTP_NOT_FOUND -> DomainError.NotFound
            else -> DomainError.Server(e.code(), e.message())
        }
        Timber.e(e, "DomainError: %s", error::class.simpleName)
        DomainResult.Failure(error)
    } catch (e: Throwable) {
        Timber.e(e, "DomainError: Unknown")
        DomainResult.Failure(DomainError.Unknown(e))
    }

/**
 * Variant for local storage operations — maps all non-cancellation exceptions to [DomainError.Storage].
 */
suspend inline fun <T> safeStorageCall(crossinline block: suspend () -> T): DomainResult<T> =
    try {
        DomainResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Timber.e(e, "DomainError: Storage")
        DomainResult.Failure(DomainError.Storage(e))
    }
```

**Step 2: Verify core:data compiles**

Run: `JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew :core:data:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add core/data/src/main/java/com/lfr/dynamicforms/data/repository/SafeCall.kt
git commit -m "feat: add safeCall and safeStorageCall helpers for DomainResult wrapping"
```

---

### Task 3: Update repository interfaces to return DomainResult

**Files:**
- Modify: `core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/repository/FormRepository.kt`
- Modify: `core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/repository/DraftRepository.kt`
- Modify: `core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/repository/SubmissionQueueRepository.kt`

**Step 1: Update FormRepository**

Replace the file:

```kotlin
package com.lfr.dynamicforms.domain.repository

import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.SubmissionResponse

interface FormRepository {
    suspend fun getForms(): DomainResult<List<FormSummary>>
    suspend fun getForm(formId: String): DomainResult<Form>
    suspend fun submitForm(formId: String, values: Map<String, String>, idempotencyKey: String? = null): DomainResult<SubmissionResponse>
}
```

**Step 2: Update DraftRepository**

Replace the file:

```kotlin
package com.lfr.dynamicforms.domain.repository

import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Draft

interface DraftRepository {
    suspend fun saveDraft(formId: String, pageIndex: Int, values: Map<String, String>): DomainResult<Unit>
    suspend fun getDraft(formId: String): DomainResult<Draft?>
    suspend fun deleteDraft(formId: String): DomainResult<Unit>
    suspend fun getFormIdsWithDrafts(): DomainResult<List<String>>
}
```

**Step 3: Update SubmissionQueueRepository**

Replace the file:

```kotlin
package com.lfr.dynamicforms.domain.repository

import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.SubmissionStatus
import kotlinx.coroutines.flow.Flow

interface SubmissionQueueRepository {
    suspend fun enqueue(formId: String, formTitle: String, values: Map<String, String>): DomainResult<String>
    fun observeAll(): Flow<List<PendingSubmission>>
    fun observePendingCountByFormId(formId: String): Flow<Int>
    suspend fun getPendingSubmissions(): DomainResult<List<PendingSubmission>>
    suspend fun updateStatus(id: String, status: SubmissionStatus, errorMessage: String? = null, attemptCount: Int? = null): DomainResult<Unit>
    suspend fun delete(id: String): DomainResult<Unit>
    suspend fun retry(id: String): DomainResult<Unit>
}
```

Note: `observeAll()` and `observePendingCountByFormId()` stay as `Flow` — they don't throw on subscribe, and Room Flow handles errors internally.

**Step 4: Do NOT compile yet** — the implementations and callers will be updated in the next tasks. This is a coordinated change.

**Step 5: Commit**

```bash
git add core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/repository/FormRepository.kt \
       core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/repository/DraftRepository.kt \
       core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/repository/SubmissionQueueRepository.kt
git commit -m "feat: update repository interfaces to return DomainResult"
```

---

### Task 4: Update repository implementations

**Files:**
- Modify: `core/data/src/main/java/com/lfr/dynamicforms/data/repository/FormRepositoryImpl.kt`
- Modify: `core/data/src/main/java/com/lfr/dynamicforms/data/repository/DraftRepositoryImpl.kt`
- Modify: `core/data/src/main/java/com/lfr/dynamicforms/data/repository/SubmissionQueueRepositoryImpl.kt`

**Step 1: Update FormRepositoryImpl**

Replace the file:

```kotlin
package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.remote.FormApi
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormSubmission
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import com.lfr.dynamicforms.domain.repository.FormRepository
import timber.log.Timber
import javax.inject.Inject

class FormRepositoryImpl @Inject constructor(
    private val api: FormApi
) : FormRepository {

    override suspend fun getForms(): DomainResult<List<FormSummary>> = safeCall {
        Timber.d("Fetching form list")
        api.getForms()
    }

    override suspend fun getForm(formId: String): DomainResult<Form> = safeCall {
        Timber.d("Fetching form: %s", formId)
        api.getForm(formId)
    }

    override suspend fun submitForm(
        formId: String,
        values: Map<String, String>,
        idempotencyKey: String?,
    ): DomainResult<SubmissionResponse> = safeCall {
        Timber.d("Submitting form: %s (key=%s)", formId, idempotencyKey)
        api.submitForm(formId, FormSubmission(formId, values), idempotencyKey)
    }
}
```

**Step 2: Update DraftRepositoryImpl**

Replace the file:

```kotlin
package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.local.DraftDao
import com.lfr.dynamicforms.data.local.DraftEntity
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Draft
import com.lfr.dynamicforms.domain.repository.DraftRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DraftRepositoryImpl @Inject constructor(
    private val draftDao: DraftDao,
    private val json: Json
) : DraftRepository {

    override suspend fun saveDraft(formId: String, pageIndex: Int, values: Map<String, String>): DomainResult<Unit> =
        safeStorageCall {
            draftDao.upsert(
                DraftEntity(
                    formId = formId,
                    pageIndex = pageIndex,
                    valuesJson = json.encodeToString(values),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

    override suspend fun getDraft(formId: String): DomainResult<Draft?> = safeStorageCall {
        val entity = draftDao.getDraft(formId) ?: return@safeStorageCall null
        val values: Map<String, String> = try {
            json.decodeFromString(entity.valuesJson)
        } catch (_: Exception) {
            draftDao.deleteDraft(formId)
            return@safeStorageCall null
        }
        Draft(
            formId = entity.formId,
            pageIndex = entity.pageIndex,
            values = values,
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun deleteDraft(formId: String): DomainResult<Unit> = safeStorageCall {
        draftDao.deleteDraft(formId)
    }

    override suspend fun getFormIdsWithDrafts(): DomainResult<List<String>> = safeStorageCall {
        draftDao.getFormIdsWithDrafts()
    }
}
```

**Step 3: Update SubmissionQueueRepositoryImpl**

Replace the file:

```kotlin
package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.local.PendingSubmissionDao
import com.lfr.dynamicforms.data.local.PendingSubmissionEntity
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.SubmissionStatus
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

class SubmissionQueueRepositoryImpl @Inject constructor(
    private val dao: PendingSubmissionDao,
    private val json: Json
) : SubmissionQueueRepository {

    override suspend fun enqueue(formId: String, formTitle: String, values: Map<String, String>): DomainResult<String> =
        safeStorageCall {
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            dao.upsert(
                PendingSubmissionEntity(
                    id = id,
                    formId = formId,
                    formTitle = formTitle,
                    valuesJson = json.encodeToString(values),
                    status = SubmissionStatus.PENDING.name,
                    errorMessage = null,
                    attemptCount = 0,
                    createdAt = now,
                    updatedAt = now
                )
            )
            id
        }

    override fun observeAll(): Flow<List<PendingSubmission>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observePendingCountByFormId(formId: String): Flow<Int> =
        dao.observePendingCountByFormId(formId)

    override suspend fun getPendingSubmissions(): DomainResult<List<PendingSubmission>> =
        safeStorageCall { dao.getPending().map { it.toDomain() } }

    override suspend fun updateStatus(
        id: String,
        status: SubmissionStatus,
        errorMessage: String?,
        attemptCount: Int?
    ): DomainResult<Unit> = safeStorageCall {
        val existing = dao.getPending().find { it.id == id }
        dao.updateStatus(
            id = id,
            status = status.name,
            errorMessage = errorMessage,
            attemptCount = attemptCount ?: existing?.attemptCount ?: 0,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun delete(id: String): DomainResult<Unit> = safeStorageCall {
        dao.delete(id)
    }

    override suspend fun retry(id: String): DomainResult<Unit> = safeStorageCall {
        dao.updateStatus(
            id = id,
            status = SubmissionStatus.PENDING.name,
            errorMessage = null,
            attemptCount = 0,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun PendingSubmissionEntity.toDomain(): PendingSubmission = PendingSubmission(
        id = id,
        formId = formId,
        formTitle = formTitle,
        values = try {
            json.decodeFromString(valuesJson)
        } catch (_: Exception) {
            emptyMap()
        },
        status = try {
            SubmissionStatus.valueOf(status)
        } catch (_: Exception) {
            SubmissionStatus.PENDING
        },
        errorMessage = errorMessage,
        attemptCount = attemptCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
```

**Step 4: Do NOT compile yet** — use cases still reference old signatures.

**Step 5: Commit**

```bash
git add core/data/src/main/java/com/lfr/dynamicforms/data/repository/FormRepositoryImpl.kt \
       core/data/src/main/java/com/lfr/dynamicforms/data/repository/DraftRepositoryImpl.kt \
       core/data/src/main/java/com/lfr/dynamicforms/data/repository/SubmissionQueueRepositoryImpl.kt
git commit -m "feat: update repository implementations to return DomainResult via safeCall"
```

---

### Task 5: Update use cases

**Files:**
- Modify: `core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/usecase/GetFormUseCase.kt`
- Modify: `core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/usecase/SubmitFormUseCase.kt`
- Modify: `core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/usecase/SyncSubmissionUseCase.kt`
- Modify: `core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/usecase/SaveDraftUseCase.kt`
- Modify: `core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/usecase/EnqueueSubmissionUseCase.kt`

**Step 1: Update GetFormUseCase**

Replace the file:

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.flatMap
import com.lfr.dynamicforms.domain.model.getDefaultValue
import com.lfr.dynamicforms.domain.model.map
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import javax.inject.Inject

data class FormWithDraft(
    val form: Form,
    val initialValues: Map<String, String>,
    val initialPageIndex: Int
)

class GetFormUseCase @Inject constructor(
    private val formRepository: FormRepository,
    private val draftRepository: DraftRepository
) {
    suspend operator fun invoke(formId: String): DomainResult<FormWithDraft> =
        formRepository.getForm(formId).flatMap { form ->
            val defaults = mutableMapOf<String, String>()
            for (page in form.pages) {
                for (element in page.elements) {
                    element.getDefaultValue()?.let { defaults[element.id] = it }
                }
            }

            draftRepository.getDraft(formId).map { draft ->
                if (draft != null) {
                    FormWithDraft(
                        form = form,
                        initialValues = defaults + draft.values,
                        initialPageIndex = draft.pageIndex
                    )
                } else {
                    FormWithDraft(
                        form = form,
                        initialValues = defaults,
                        initialPageIndex = 0
                    )
                }
            }
        }
}
```

**Step 2: Update SubmitFormUseCase** (delete SubmitResult)

Replace the file:

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import com.lfr.dynamicforms.domain.model.flatMap
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import javax.inject.Inject

class SubmitFormUseCase @Inject constructor(
    private val formRepository: FormRepository,
    private val draftRepository: DraftRepository,
    private val validatePage: ValidatePageUseCase
) {
    suspend operator fun invoke(
        form: Form,
        values: Map<String, String>
    ): DomainResult<SubmissionResponse> {
        val errors = validatePage.validateAllPages(form.pages, values)
        if (errors.isNotEmpty()) {
            return DomainResult.Failure(DomainError.Validation(errors))
        }

        return formRepository.submitForm(form.formId, values).flatMap { response ->
            if (response.success) {
                draftRepository.deleteDraft(form.formId)
                DomainResult.Success(response)
            } else {
                DomainResult.Success(response)
            }
        }
    }
}
```

Note: When the server returns `success=false` with `fieldErrors`, this is still a successful HTTP response. The caller (ViewModel) inspects `response.success` and `response.fieldErrors` to decide how to display.

**Step 3: Update SyncSubmissionUseCase** (delete SyncResult)

Replace the file:

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.SubmissionStatus
import com.lfr.dynamicforms.domain.model.fold
import com.lfr.dynamicforms.domain.repository.FormRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import javax.inject.Inject

/**
 * Result of a single submission sync attempt.
 */
sealed class SyncResult {
    data object Synced : SyncResult()
    data object Failed : SyncResult()
    data object Retryable : SyncResult()
}

class SyncSubmissionUseCase @Inject constructor(
    private val submissionQueueRepository: SubmissionQueueRepository,
    private val formRepository: FormRepository
) {
    suspend operator fun invoke(submission: PendingSubmission): SyncResult {
        submissionQueueRepository.updateStatus(submission.id, SubmissionStatus.SYNCING)

        return formRepository.submitForm(
            submission.formId, submission.values, idempotencyKey = submission.id
        ).fold(
            onSuccess = { response ->
                if (response.success) {
                    submissionQueueRepository.delete(submission.id)
                    SyncResult.Synced
                } else {
                    val errorSummary = response.fieldErrors.entries.joinToString("; ") { "${it.key}: ${it.value}" }
                    submissionQueueRepository.updateStatus(
                        submission.id,
                        SubmissionStatus.FAILED,
                        errorMessage = response.message ?: errorSummary,
                        attemptCount = submission.attemptCount + 1
                    )
                    SyncResult.Failed
                }
            },
            onFailure = { error ->
                val newAttemptCount = submission.attemptCount + 1
                val isTransient = error is DomainError.Network || error is DomainError.Timeout
                if (newAttemptCount >= MAX_ATTEMPTS || !isTransient) {
                    val errorMessage = when (error) {
                        is DomainError.Network -> error.cause?.message ?: "Network error"
                        is DomainError.Timeout -> error.cause?.message ?: "Request timed out"
                        is DomainError.Server -> error.message ?: "Server error (${error.code})"
                        else -> "Sync failed"
                    }
                    submissionQueueRepository.updateStatus(
                        submission.id,
                        SubmissionStatus.FAILED,
                        errorMessage = errorMessage,
                        attemptCount = newAttemptCount
                    )
                    SyncResult.Failed
                } else {
                    submissionQueueRepository.updateStatus(
                        submission.id,
                        SubmissionStatus.PENDING,
                        attemptCount = newAttemptCount
                    )
                    SyncResult.Retryable
                }
            }
        )
    }

    companion object {
        const val MAX_ATTEMPTS = 5
    }
}
```

Note: `SyncResult` stays — it's a use-case-specific return type for the worker, not an error type. The key change is the interior: `formRepository.submitForm()` now returns `DomainResult` so we use `fold` instead of try-catch.

**Step 4: Update SaveDraftUseCase**

Replace the file:

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.repository.DraftRepository
import javax.inject.Inject

class SaveDraftUseCase @Inject constructor(
    private val draftRepository: DraftRepository
) {
    suspend operator fun invoke(formId: String, pageIndex: Int, values: Map<String, String>): DomainResult<Unit> =
        draftRepository.saveDraft(formId, pageIndex, values)
}
```

**Step 5: Update EnqueueSubmissionUseCase**

Replace the file:

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.flatMap
import com.lfr.dynamicforms.domain.model.map
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import javax.inject.Inject

class EnqueueSubmissionUseCase @Inject constructor(
    private val submissionQueueRepository: SubmissionQueueRepository,
    private val draftRepository: DraftRepository,
    private val workScheduler: SyncWorkScheduler
) {
    suspend operator fun invoke(formId: String, formTitle: String, values: Map<String, String>): DomainResult<String> =
        submissionQueueRepository.enqueue(formId, formTitle, values).map { submissionId ->
            draftRepository.deleteDraft(formId)
            workScheduler.scheduleSync()
            submissionId
        }
}
```

**Step 6: Do NOT compile yet** — ViewModels still reference old patterns.

**Step 7: Commit**

```bash
git add core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/usecase/GetFormUseCase.kt \
       core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/usecase/SubmitFormUseCase.kt \
       core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/usecase/SyncSubmissionUseCase.kt \
       core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/usecase/SaveDraftUseCase.kt \
       core/domain/src/main/kotlin/com/lfr/dynamicforms/domain/usecase/EnqueueSubmissionUseCase.kt
git commit -m "feat: update use cases to compose DomainResult without try-catch"
```

---

### Task 6: Update ErrorMapping to accept DomainError

**Files:**
- Modify: `core/ui/src/main/java/com/lfr/dynamicforms/presentation/util/ErrorMapping.kt`

**Step 1: Replace ErrorMapping.kt**

```kotlin
package com.lfr.dynamicforms.presentation.util

import com.lfr.dynamicforms.domain.model.DomainError

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

**Step 2: Commit**

```bash
git add core/ui/src/main/java/com/lfr/dynamicforms/presentation/util/ErrorMapping.kt
git commit -m "feat: update ErrorMapping to accept DomainError instead of Throwable"
```

---

### Task 7: Update FormViewModel

**Files:**
- Modify: `feature/form-wizard/src/main/java/com/lfr/dynamicforms/presentation/form/FormViewModel.kt`

**Step 1: Replace FormViewModel.kt**

```kotlin
package com.lfr.dynamicforms.presentation.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.RepeatingGroupElement
import com.lfr.dynamicforms.domain.model.fold
import com.lfr.dynamicforms.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.lfr.dynamicforms.presentation.util.toUserMessage
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getForm: GetFormUseCase,
    private val saveDraft: SaveDraftUseCase,
    @Suppress("unused") private val submitForm: SubmitFormUseCase,
    private val enqueueSubmission: EnqueueSubmissionUseCase,
    private val validatePage: ValidatePageUseCase,
    private val evaluateVisibility: EvaluateVisibilityUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FormUiState())
    val state: StateFlow<FormUiState> = _state.asStateFlow()

    private val _effect = Channel<FormEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var draftDebounceJob: Job? = null

    companion object {
        private const val DRAFT_SAVE_DEBOUNCE_MS = 2000L
    }

    init {
        val formId = savedStateHandle.get<String>("formId")
        if (formId != null) loadForm(formId)
    }

    fun onAction(action: FormAction) {
        when (action) {
            is FormAction.LoadForm -> loadForm(action.formId)
            is FormAction.UpdateField -> updateField(action.fieldId, action.value)
            is FormAction.NextPage -> nextPage()
            is FormAction.PrevPage -> prevPage()
            is FormAction.Submit -> submit()
            is FormAction.SaveDraft -> saveDraftNow()
            is FormAction.AddRepeatingRow -> addRow(action.groupId)
            is FormAction.RemoveRepeatingRow -> removeRow(action.groupId, action.rowIndex)
        }
    }

    private fun loadForm(formId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, formId = formId) }

            getForm(formId).fold(
                onSuccess = { result ->
                    val groupCounts = mutableMapOf<String, Int>()
                    for (page in result.form.pages) {
                        for (element in page.elements) {
                            if (element is RepeatingGroupElement) {
                                groupCounts[element.id] = element.minItems
                            }
                        }
                    }
                    val visibleIds = computeVisibleIds(result.form, result.initialValues)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            form = result.form,
                            values = result.initialValues,
                            currentPageIndex = result.initialPageIndex,
                            repeatingGroupCounts = groupCounts,
                            visibleElementIds = visibleIds
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.toUserMessage()) }
                }
            )
        }
    }

    private fun updateField(fieldId: String, value: String) {
        _state.update { current ->
            val newValues = current.values + (fieldId to value)
            val newErrors = current.errors - fieldId
            val visibleIds = computeVisibleIds(current.form, newValues)
            current.copy(values = newValues, errors = newErrors, visibleElementIds = visibleIds)
        }
        scheduleDraftSave()
    }

    private fun computeVisibleIds(form: Form?, values: Map<String, String>): Set<String> {
        if (form == null) return emptySet()
        return form.pages.flatMap { page ->
            evaluateVisibility.getVisibleElements(page, values).map { it.id }
        }.toSet()
    }

    private fun nextPage() {
        val current = _state.value
        val page = current.currentPage ?: return
        val errors = validatePage.validate(page, current.values)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = it.errors + errors) }
            return
        }
        _state.update { it.copy(currentPageIndex = it.currentPageIndex + 1, errors = emptyMap()) }
        saveDraftNow()
    }

    private fun prevPage() {
        _state.update { it.copy(currentPageIndex = (it.currentPageIndex - 1).coerceAtLeast(0)) }
        saveDraftNow()
    }

    private fun submit() {
        val current = _state.value
        val form = current.form ?: return

        val errors = validatePage.validateAllPages(form.pages, current.values)
        if (errors.isNotEmpty()) {
            val firstPage = validatePage.firstPageWithErrors(form.pages, errors)
            _state.update {
                it.copy(errors = errors, currentPageIndex = firstPage)
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            enqueueSubmission(form.formId, form.title, current.values).fold(
                onSuccess = { _ ->
                    _state.update { it.copy(isSubmitting = false) }
                    _effect.send(FormEffect.NavigateToSuccess(form.formId, "Form queued for submission"))
                },
                onFailure = { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    _effect.send(FormEffect.ShowError(error.toUserMessage()))
                }
            )
        }
    }

    private fun scheduleDraftSave() {
        draftDebounceJob?.cancel()
        draftDebounceJob = viewModelScope.launch {
            delay(DRAFT_SAVE_DEBOUNCE_MS)
            saveDraftNow()
        }
    }

    override fun onCleared() {
        draftDebounceJob?.cancel()
        saveDraftNow()
    }

    private fun saveDraftNow() {
        val current = _state.value
        val formId = current.form?.formId ?: return
        viewModelScope.launch {
            saveDraft(formId, current.currentPageIndex, current.values).fold(
                onSuccess = { },
                onFailure = { error -> Timber.w("Draft save failed: %s", error) }
            )
        }
    }

    private fun addRow(groupId: String) {
        _state.update { current ->
            val counts = current.repeatingGroupCounts.toMutableMap()
            counts[groupId] = (counts[groupId] ?: 1) + 1
            current.copy(repeatingGroupCounts = counts)
        }
    }

    private fun removeRow(groupId: String, rowIndex: Int) {
        _state.update { current ->
            val counts = current.repeatingGroupCounts.toMutableMap()
            val count = counts[groupId] ?: return@update current
            counts[groupId] = (count - 1).coerceAtLeast(0)

            val newValues = current.values.toMutableMap()
            val prefix = "$groupId[$rowIndex]."
            newValues.keys.removeAll { it.startsWith(prefix) }

            for (i in (rowIndex + 1) until count) {
                val oldPrefix = "$groupId[$i]."
                val newPrefix = "$groupId[${i - 1}]."
                val keysToShift = newValues.keys.filter { it.startsWith(oldPrefix) }
                for (key in keysToShift) {
                    val value = newValues.remove(key) ?: continue
                    newValues[key.replaceFirst(oldPrefix, newPrefix)] = value
                }
            }

            current.copy(values = newValues, repeatingGroupCounts = counts)
        }
    }
}
```

**Step 2: Commit**

```bash
git add feature/form-wizard/src/main/java/com/lfr/dynamicforms/presentation/form/FormViewModel.kt
git commit -m "feat: update FormViewModel to use DomainResult.fold instead of try-catch"
```

---

### Task 8: Update FormListViewModel with FormListEffect

**Files:**
- Modify: `feature/form-list/src/main/java/com/lfr/dynamicforms/presentation/list/FormListViewModel.kt`
- Modify: `feature/form-list/src/main/java/com/lfr/dynamicforms/presentation/list/FormListScreen.kt`

**Step 1: Replace FormListViewModel.kt**

```kotlin
package com.lfr.dynamicforms.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.fold
import com.lfr.dynamicforms.domain.model.map
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import com.lfr.dynamicforms.domain.usecase.SyncWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.lfr.dynamicforms.presentation.util.toUserMessage
import javax.inject.Inject

data class FormListState(
    val isLoading: Boolean = true,
    val forms: List<FormSummary> = emptyList(),
    val drafts: Set<String> = emptySet(),
    val pendingSubmissions: List<PendingSubmission> = emptyList(),
    val pendingCountsByFormId: Map<String, Int> = emptyMap(),
    val errorMessage: String? = null
)

sealed interface FormListEffect {
    data class ShowError(val message: String) : FormListEffect
}

@HiltViewModel
class FormListViewModel @Inject constructor(
    private val formRepository: FormRepository,
    private val draftRepository: DraftRepository,
    private val submissionQueueRepository: SubmissionQueueRepository,
    private val syncWorkScheduler: SyncWorkScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(FormListState())
    val state: StateFlow<FormListState> = _state.asStateFlow()

    private val _effect = Channel<FormListEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadForms()
        observePendingSubmissions()
    }

    fun refresh() = loadForms()

    fun retrySubmission(id: String) {
        viewModelScope.launch {
            submissionQueueRepository.retry(id).fold(
                onSuccess = { syncWorkScheduler.scheduleSync() },
                onFailure = { error -> _effect.send(FormListEffect.ShowError(error.toUserMessage())) }
            )
        }
    }

    fun discardSubmission(id: String) {
        viewModelScope.launch {
            submissionQueueRepository.delete(id).fold(
                onSuccess = { },
                onFailure = { error -> _effect.send(FormListEffect.ShowError(error.toUserMessage())) }
            )
        }
    }

    private fun loadForms() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            formRepository.getForms().fold(
                onSuccess = { forms ->
                    draftRepository.getFormIdsWithDrafts().fold(
                        onSuccess = { drafts ->
                            _state.update { it.copy(isLoading = false, forms = forms, drafts = drafts.toSet()) }
                        },
                        onFailure = {
                            _state.update { it.copy(isLoading = false, forms = forms, drafts = emptySet()) }
                        }
                    )
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.toUserMessage()) }
                }
            )
        }
    }

    private fun observePendingSubmissions() {
        viewModelScope.launch {
            submissionQueueRepository.observeAll().collect { submissions ->
                val countsByForm = submissions.groupBy { it.formId }
                    .mapValues { it.value.size }
                _state.update {
                    it.copy(
                        pendingSubmissions = submissions,
                        pendingCountsByFormId = countsByForm
                    )
                }
            }
        }
    }
}
```

**Step 2: Update FormListScreen to collect FormListEffect**

In `FormListScreen.kt`, update the `FormListScreen` composable (lines 47-61) to:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormListScreen(
    onFormClick: (String) -> Unit,
    viewModel: FormListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FormListEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    FormListScreenContent(
        state = state,
        onFormClick = onFormClick,
        onRetry = { viewModel.refresh() },
        onRetrySubmission = { viewModel.retrySubmission(it) },
        onDiscardSubmission = { viewModel.discardSubmission(it) },
        snackbarHostState = snackbarHostState
    )
}
```

And update `FormListScreenContent` to accept and wire up `snackbarHostState`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormListScreenContent(
    state: FormListState,
    onFormClick: (String) -> Unit,
    onRetry: () -> Unit = {},
    onRetrySubmission: (String) -> Unit = {},
    onDiscardSubmission: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.form_list_title)) }) }
    ) { padding ->
```

Add these imports to `FormListScreen.kt`:

```kotlin
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
```

**Step 3: Commit**

```bash
git add feature/form-list/src/main/java/com/lfr/dynamicforms/presentation/list/FormListViewModel.kt \
       feature/form-list/src/main/java/com/lfr/dynamicforms/presentation/list/FormListScreen.kt
git commit -m "feat: update FormListViewModel with DomainResult and add FormListEffect"
```

---

### Task 9: Update SyncSubmissionsWorker

**Files:**
- Modify: `core/data/src/main/java/com/lfr/dynamicforms/data/worker/SyncSubmissionsWorker.kt`

**Step 1: Replace SyncSubmissionsWorker.kt**

```kotlin
package com.lfr.dynamicforms.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lfr.dynamicforms.domain.model.fold
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import com.lfr.dynamicforms.domain.usecase.SyncResult
import com.lfr.dynamicforms.domain.usecase.SyncSubmissionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SyncSubmissionsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncSubmission: SyncSubmissionUseCase,
    private val submissionQueueRepository: SubmissionQueueRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = submissionQueueRepository.getPendingSubmissions().fold(
            onSuccess = { it },
            onFailure = { error ->
                Timber.e("SyncSubmissionsWorker: failed to fetch pending submissions: %s", error)
                return Result.retry()
            }
        )
        Timber.d("SyncSubmissionsWorker: %d pending submissions", pending.size)

        var hasRetryable = false
        for (submission in pending) {
            Timber.d("Syncing submission %s for form %s", submission.id, submission.formId)
            when (syncSubmission(submission)) {
                SyncResult.Retryable -> hasRetryable = true
                else -> { }
            }
        }

        Timber.d("SyncSubmissionsWorker: completed (hasRetryable=%s)", hasRetryable)
        return if (hasRetryable) Result.retry() else Result.success()
    }
}
```

**Step 2: Compile the full project**

Run: `JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add core/data/src/main/java/com/lfr/dynamicforms/data/worker/SyncSubmissionsWorker.kt
git commit -m "feat: update SyncSubmissionsWorker to use Result.retry for transient errors"
```

---

### Task 10: Update repository tests

**Files:**
- Modify: `core/data/src/test/java/com/lfr/dynamicforms/data/repository/FormRepositoryImplTest.kt`
- Modify: `core/data/src/test/java/com/lfr/dynamicforms/data/repository/DraftRepositoryImplTest.kt`
- Modify: `core/data/src/test/java/com/lfr/dynamicforms/data/repository/SubmissionQueueRepositoryImplTest.kt`

**Step 1: Replace FormRepositoryImplTest.kt**

```kotlin
package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.remote.FormApi
import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormSubmission
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class FormRepositoryImplTest {

    private val api = mockk<FormApi>(relaxUnitFun = true)
    private val repository = FormRepositoryImpl(api)

    @Test
    fun `getForms returns Success with forms`() = runTest {
        val expected = listOf(
            FormSummary(formId = "f1", title = "Form One"),
            FormSummary(formId = "f2", title = "Form Two")
        )
        coEvery { api.getForms() } returns expected

        val result = repository.getForms()

        assertTrue(result is DomainResult.Success)
        assertEquals(expected, (result as DomainResult.Success).data)
    }

    @Test
    fun `getForm returns Success with form`() = runTest {
        val expected = Form(formId = "f1", title = "Form One", pages = emptyList())
        coEvery { api.getForm("f1") } returns expected

        val result = repository.getForm("f1")

        assertTrue(result is DomainResult.Success)
        assertEquals(expected, (result as DomainResult.Success).data)
    }

    @Test
    fun `submitForm wraps values in FormSubmission`() = runTest {
        val submissionSlot = slot<FormSubmission>()
        coEvery { api.submitForm(any(), capture(submissionSlot), any()) } returns SubmissionResponse(
            success = true,
            message = "OK"
        )

        val result = repository.submitForm("f1", mapOf("k" to "v"))

        assertTrue(result is DomainResult.Success)
        coVerify { api.submitForm("f1", any(), any()) }
        val captured = submissionSlot.captured
        assertEquals("f1", captured.formId)
        assertEquals(mapOf("k" to "v"), captured.values)
    }

    @Test
    fun `submitForm passes idempotency key to api`() = runTest {
        coEvery { api.submitForm(any(), any(), any()) } returns SubmissionResponse(
            success = true,
            message = "OK"
        )

        repository.submitForm("f1", mapOf("k" to "v"), idempotencyKey = "key-123")

        coVerify { api.submitForm("f1", any(), "key-123") }
    }

    @Test
    fun `getForm returns Failure with Network error on UnknownHostException`() = runTest {
        coEvery { api.getForm("x") } throws UnknownHostException("no host")

        val result = repository.getForm("x")

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Network)
    }

    @Test
    fun `getForm returns Failure with Unknown error on RuntimeException`() = runTest {
        coEvery { api.getForm("x") } throws RuntimeException("fail")

        val result = repository.getForm("x")

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Unknown)
    }
}
```

**Step 2: Replace DraftRepositoryImplTest.kt**

```kotlin
package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.local.DraftDao
import com.lfr.dynamicforms.data.local.DraftEntity
import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftRepositoryImplTest {

    private val draftDao = mockk<DraftDao>(relaxUnitFun = true)
    private val json = Json { ignoreUnknownKeys = true }
    private val repository = DraftRepositoryImpl(draftDao, json)

    @Test
    fun `saveDraft serializes values to JSON entity`() = runTest {
        val entitySlot = slot<DraftEntity>()
        coEvery { draftDao.upsert(capture(entitySlot)) } returns Unit

        val result = repository.saveDraft("f1", 2, mapOf("name" to "Jane"))

        assertTrue(result is DomainResult.Success)
        coVerify { draftDao.upsert(any()) }
        val captured = entitySlot.captured
        assertEquals("f1", captured.formId)
        assertEquals(2, captured.pageIndex)
        val deserializedValues = Json.decodeFromString<Map<String, String>>(captured.valuesJson)
        assertEquals(mapOf("name" to "Jane"), deserializedValues)
    }

    @Test
    fun `getDraft deserializes entity to Draft`() = runTest {
        val entity = DraftEntity(
            formId = "f1",
            pageIndex = 2,
            valuesJson = """{"name":"Jane"}""",
            updatedAt = 1000L
        )
        coEvery { draftDao.getDraft("f1") } returns entity

        val result = repository.getDraft("f1")

        assertTrue(result is DomainResult.Success)
        val draft = (result as DomainResult.Success).data!!
        assertEquals("f1", draft.formId)
        assertEquals(2, draft.pageIndex)
        assertEquals(mapOf("name" to "Jane"), draft.values)
        assertEquals(1000L, draft.updatedAt)
    }

    @Test
    fun `getDraft returns Success null when no entity`() = runTest {
        coEvery { draftDao.getDraft("f1") } returns null

        val result = repository.getDraft("f1")

        assertTrue(result is DomainResult.Success)
        assertNull((result as DomainResult.Success).data)
    }

    @Test
    fun `getDraft returns Success null and deletes draft when JSON is corrupted`() = runTest {
        val entity = DraftEntity(
            formId = "f1",
            pageIndex = 0,
            valuesJson = "not valid json {{{",
            updatedAt = 1000L
        )
        coEvery { draftDao.getDraft("f1") } returns entity

        val result = repository.getDraft("f1")

        assertTrue(result is DomainResult.Success)
        assertNull((result as DomainResult.Success).data)
        coVerify { draftDao.deleteDraft("f1") }
    }

    @Test
    fun `saveDraft returns Failure with Storage error on exception`() = runTest {
        coEvery { draftDao.upsert(any()) } throws RuntimeException("DB error")

        val result = repository.saveDraft("f1", 0, emptyMap())

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Storage)
    }
}
```

**Step 3: Replace SubmissionQueueRepositoryImplTest.kt**

```kotlin
package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.local.PendingSubmissionDao
import com.lfr.dynamicforms.data.local.PendingSubmissionEntity
import com.lfr.dynamicforms.domain.model.DomainResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionQueueRepositoryImplTest {

    private val dao = mockk<PendingSubmissionDao>(relaxUnitFun = true)
    private val json = Json { ignoreUnknownKeys = true }
    private val repository = SubmissionQueueRepositoryImpl(dao, json)

    @Test
    fun `enqueue creates entity with UUID and PENDING status`() = runTest {
        val entitySlot = slot<PendingSubmissionEntity>()
        coEvery { dao.upsert(capture(entitySlot)) } returns Unit

        val result = repository.enqueue("f1", "Test Form", mapOf("name" to "Jane"))

        assertTrue(result is DomainResult.Success)
        val id = (result as DomainResult.Success).data
        assertNotNull(id)
        val captured = entitySlot.captured
        assertEquals(id, captured.id)
        assertEquals("f1", captured.formId)
        assertEquals("Test Form", captured.formTitle)
        assertEquals("PENDING", captured.status)
        assertEquals(0, captured.attemptCount)
        val values: Map<String, String> = Json.decodeFromString(captured.valuesJson)
        assertEquals(mapOf("name" to "Jane"), values)
    }

    @Test
    fun `retry resets status and attemptCount`() = runTest {
        val result = repository.retry("sub-1")

        assertTrue(result is DomainResult.Success)
        coVerify {
            dao.updateStatus(
                id = "sub-1",
                status = "PENDING",
                errorMessage = null,
                attemptCount = 0,
                updatedAt = any()
            )
        }
    }

    @Test
    fun `delete removes entity`() = runTest {
        val result = repository.delete("sub-1")

        assertTrue(result is DomainResult.Success)
        coVerify { dao.delete("sub-1") }
    }
}
```

**Step 4: Run repository tests**

Run: `JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew :core:data:test`
Expected: All tests pass

**Step 5: Commit**

```bash
git add core/data/src/test/java/com/lfr/dynamicforms/data/repository/FormRepositoryImplTest.kt \
       core/data/src/test/java/com/lfr/dynamicforms/data/repository/DraftRepositoryImplTest.kt \
       core/data/src/test/java/com/lfr/dynamicforms/data/repository/SubmissionQueueRepositoryImplTest.kt
git commit -m "test: update repository tests for DomainResult return types"
```

---

### Task 11: Update use case tests

**Files:**
- Modify: `core/domain/src/test/kotlin/com/lfr/dynamicforms/domain/usecase/GetFormUseCaseTest.kt`
- Modify: `core/domain/src/test/kotlin/com/lfr/dynamicforms/domain/usecase/SubmitFormUseCaseTest.kt`
- Modify: `core/domain/src/test/kotlin/com/lfr/dynamicforms/domain/usecase/SyncSubmissionUseCaseTest.kt`
- Modify: `core/domain/src/test/kotlin/com/lfr/dynamicforms/domain/usecase/SaveDraftUseCaseTest.kt`
- Modify: `core/domain/src/test/kotlin/com/lfr/dynamicforms/domain/usecase/EnqueueSubmissionUseCaseTest.kt`

**Step 1: Replace GetFormUseCaseTest.kt**

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Draft
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.Page
import com.lfr.dynamicforms.domain.model.TextFieldElement
import com.lfr.dynamicforms.domain.model.ToggleElement
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetFormUseCaseTest {

    private val formRepo = mockk<FormRepository>()
    private val draftRepo = mockk<DraftRepository>()
    private val useCase = GetFormUseCase(formRepo, draftRepo)

    @Test
    fun `returns form with default values when no draft`() = runTest {
        val toggle = ToggleElement(id = "toggle1", label = "Accept", defaultValue = true)
        val textField = TextFieldElement(id = "text1", label = "Name")
        val form = Form(
            formId = "f1",
            title = "Test Form",
            pages = listOf(Page(pageId = "p1", title = "Page 1", elements = listOf(toggle, textField)))
        )

        coEvery { formRepo.getForm("f1") } returns DomainResult.Success(form)
        coEvery { draftRepo.getDraft("f1") } returns DomainResult.Success(null)

        val result = useCase("f1")

        assertTrue(result is DomainResult.Success)
        val data = (result as DomainResult.Success).data
        assertEquals(form, data.form)
        assertEquals("true", data.initialValues["toggle1"])
        assertTrue("text1" !in data.initialValues)
        assertEquals(0, data.initialPageIndex)
    }

    @Test
    fun `draft values override defaults`() = runTest {
        val toggle = ToggleElement(id = "toggle1", label = "Accept", defaultValue = true)
        val form = Form(
            formId = "f1",
            title = "Test Form",
            pages = listOf(Page(pageId = "p1", title = "Page 1", elements = listOf(toggle)))
        )
        val draft = Draft(
            formId = "f1",
            pageIndex = 0,
            values = mapOf("toggle1" to "false"),
            updatedAt = 1000L
        )

        coEvery { formRepo.getForm("f1") } returns DomainResult.Success(form)
        coEvery { draftRepo.getDraft("f1") } returns DomainResult.Success(draft)

        val result = useCase("f1") as DomainResult.Success

        assertEquals("false", result.data.initialValues["toggle1"])
    }

    @Test
    fun `draft restores page index`() = runTest {
        val form = Form(
            formId = "f1",
            title = "Test Form",
            pages = listOf(Page(pageId = "p1", title = "Page 1", elements = emptyList()))
        )
        val draft = Draft(
            formId = "f1",
            pageIndex = 2,
            values = emptyMap(),
            updatedAt = 1000L
        )

        coEvery { formRepo.getForm("f1") } returns DomainResult.Success(form)
        coEvery { draftRepo.getDraft("f1") } returns DomainResult.Success(draft)

        val result = useCase("f1") as DomainResult.Success

        assertEquals(2, result.data.initialPageIndex)
    }

    @Test
    fun `non-defaultable elements excluded from defaults`() = runTest {
        val textField = TextFieldElement(id = "text1", label = "Name")
        val form = Form(
            formId = "f1",
            title = "Test Form",
            pages = listOf(Page(pageId = "p1", title = "Page 1", elements = listOf(textField)))
        )

        coEvery { formRepo.getForm("f1") } returns DomainResult.Success(form)
        coEvery { draftRepo.getDraft("f1") } returns DomainResult.Success(null)

        val result = useCase("f1") as DomainResult.Success

        assertTrue(result.data.initialValues.isEmpty())
    }

    @Test
    fun `form repository failure propagates as Failure`() = runTest {
        coEvery { formRepo.getForm("f1") } returns DomainResult.Failure(DomainError.Network())

        val result = useCase("f1")

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Network)
    }

    @Test
    fun `draft repository failure propagates as Failure`() = runTest {
        val form = Form(formId = "f1", title = "Test", pages = emptyList())
        coEvery { formRepo.getForm("f1") } returns DomainResult.Success(form)
        coEvery { draftRepo.getDraft("f1") } returns DomainResult.Failure(DomainError.Storage())

        val result = useCase("f1")

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Storage)
    }
}
```

**Step 2: Replace SubmitFormUseCaseTest.kt**

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.Page
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import com.lfr.dynamicforms.domain.model.TextFieldElement
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitFormUseCaseTest {

    private val formRepo = mockk<FormRepository>()
    private val draftRepo = mockk<DraftRepository>()
    private val validatePage = mockk<ValidatePageUseCase>()
    private val useCase = SubmitFormUseCase(formRepo, draftRepo, validatePage)

    private val form = Form(
        formId = "f1",
        title = "Test Form",
        pages = listOf(
            Page(pageId = "p1", title = "Page 1", elements = listOf(TextFieldElement(id = "name", label = "Name"))),
            Page(pageId = "p2", title = "Page 2", elements = listOf(TextFieldElement(id = "email", label = "Email")))
        )
    )

    private val values = mapOf("name" to "Jane", "email" to "jane@test.com")

    @Test
    fun `validation errors return Failure with Validation error`() = runTest {
        val errors = mapOf("name" to "Required")
        every { validatePage.validateAllPages(form.pages, values) } returns errors

        val result = useCase(form, values)

        assertTrue(result is DomainResult.Failure)
        val error = (result as DomainResult.Failure).error
        assertTrue(error is DomainError.Validation)
        assertEquals(errors, (error as DomainError.Validation).fieldErrors)
        coVerify(exactly = 0) { formRepo.submitForm(any(), any(), any()) }
    }

    @Test
    fun `successful submission returns Success and deletes draft`() = runTest {
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns DomainResult.Success(
            SubmissionResponse(success = true, message = "Done")
        )
        coEvery { draftRepo.deleteDraft("f1") } returns DomainResult.Success(Unit)

        val result = useCase(form, values)

        assertTrue(result is DomainResult.Success)
        val response = (result as DomainResult.Success).data
        assertTrue(response.success)
        assertEquals("Done", response.message)
        coVerify { draftRepo.deleteDraft("f1") }
    }

    @Test
    fun `server error returns Success with fieldErrors`() = runTest {
        val fieldErrors = mapOf("name" to "too short")
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns DomainResult.Success(
            SubmissionResponse(success = false, fieldErrors = fieldErrors)
        )

        val result = useCase(form, values)

        assertTrue(result is DomainResult.Success)
        val response = (result as DomainResult.Success).data
        assertEquals(false, response.success)
        assertEquals(fieldErrors, response.fieldErrors)
    }

    @Test
    fun `network error from repo propagates as Failure`() = runTest {
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns DomainResult.Failure(DomainError.Network())

        val result = useCase(form, values)

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Network)
        coVerify(exactly = 0) { draftRepo.deleteDraft(any()) }
    }

    @Test
    fun `draft not deleted on server error`() = runTest {
        val fieldErrors = mapOf("name" to "too short")
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns DomainResult.Success(
            SubmissionResponse(success = false, fieldErrors = fieldErrors)
        )

        useCase(form, values)

        coVerify(exactly = 0) { draftRepo.deleteDraft(any()) }
    }
}
```

**Step 3: Replace SyncSubmissionUseCaseTest.kt**

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import com.lfr.dynamicforms.domain.model.SubmissionStatus
import com.lfr.dynamicforms.domain.repository.FormRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncSubmissionUseCaseTest {

    private val submissionQueueRepo = mockk<SubmissionQueueRepository>(relaxUnitFun = true)
    private val formRepo = mockk<FormRepository>()
    private val useCase = SyncSubmissionUseCase(submissionQueueRepo, formRepo)

    init {
        // Default relaxed returns for updateStatus and delete
        coEvery { submissionQueueRepo.updateStatus(any(), any(), any(), any()) } returns DomainResult.Success(Unit)
        coEvery { submissionQueueRepo.delete(any()) } returns DomainResult.Success(Unit)
    }

    private fun submission(attemptCount: Int = 0) = PendingSubmission(
        id = "sub-1",
        formId = "f1",
        formTitle = "Test Form",
        values = mapOf("name" to "Jane"),
        status = SubmissionStatus.PENDING,
        errorMessage = null,
        attemptCount = attemptCount,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Test
    fun `sets SYNCING before API call`() = runTest {
        coEvery { formRepo.submitForm("f1", any(), idempotencyKey = "sub-1") } returns
                DomainResult.Success(SubmissionResponse(success = true, message = "OK"))

        useCase(submission())

        coVerify { submissionQueueRepo.updateStatus("sub-1", SubmissionStatus.SYNCING) }
    }

    @Test
    fun `success deletes from queue and returns Synced`() = runTest {
        coEvery { formRepo.submitForm("f1", any(), idempotencyKey = "sub-1") } returns
                DomainResult.Success(SubmissionResponse(success = true, message = "OK"))

        val result = useCase(submission())

        assertEquals(SyncResult.Synced, result)
        coVerify { submissionQueueRepo.delete("sub-1") }
    }

    @Test
    fun `server validation error marks FAILED and returns Failed`() = runTest {
        coEvery { formRepo.submitForm("f1", any(), idempotencyKey = "sub-1") } returns
                DomainResult.Success(SubmissionResponse(success = false, message = "Validation failed", fieldErrors = mapOf("name" to "too short")))

        val result = useCase(submission())

        assertEquals(SyncResult.Failed, result)
        coVerify {
            submissionQueueRepo.updateStatus(
                "sub-1",
                SubmissionStatus.FAILED,
                errorMessage = "Validation failed",
                attemptCount = 1
            )
        }
    }

    @Test
    fun `network error with attempt less than 5 stays PENDING and returns Retryable`() = runTest {
        coEvery { formRepo.submitForm("f1", any(), idempotencyKey = "sub-1") } returns
                DomainResult.Failure(DomainError.Network(RuntimeException("Timeout")))

        val result = useCase(submission(attemptCount = 2))

        assertEquals(SyncResult.Retryable, result)
        coVerify {
            submissionQueueRepo.updateStatus(
                "sub-1",
                SubmissionStatus.PENDING,
                attemptCount = 3
            )
        }
    }

    @Test
    fun `network error at attempt 4 (becomes 5) marks FAILED`() = runTest {
        coEvery { formRepo.submitForm("f1", any(), idempotencyKey = "sub-1") } returns
                DomainResult.Failure(DomainError.Network(RuntimeException("Timeout")))

        val result = useCase(submission(attemptCount = 4))

        assertEquals(SyncResult.Failed, result)
        coVerify {
            submissionQueueRepo.updateStatus(
                "sub-1",
                SubmissionStatus.FAILED,
                errorMessage = "Timeout",
                attemptCount = 5
            )
        }
    }

    @Test
    fun `server error marks FAILED immediately regardless of attempt count`() = runTest {
        coEvery { formRepo.submitForm("f1", any(), idempotencyKey = "sub-1") } returns
                DomainResult.Failure(DomainError.Server(500, "Internal Server Error"))

        val result = useCase(submission(attemptCount = 0))

        assertEquals(SyncResult.Failed, result)
        coVerify {
            submissionQueueRepo.updateStatus(
                "sub-1",
                SubmissionStatus.FAILED,
                errorMessage = "Internal Server Error",
                attemptCount = 1
            )
        }
    }
}
```

**Step 4: Replace SaveDraftUseCaseTest.kt**

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.repository.DraftRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveDraftUseCaseTest {

    private val draftRepo = mockk<DraftRepository>()
    private val useCase = SaveDraftUseCase(draftRepo)

    @Test
    fun `delegates to repository with correct parameters`() = runTest {
        val values = mapOf("name" to "Alice", "email" to "alice@test.com")
        coEvery { draftRepo.saveDraft("f1", 2, values) } returns DomainResult.Success(Unit)

        val result = useCase("f1", 2, values)

        assertTrue(result is DomainResult.Success)
        coVerify { draftRepo.saveDraft("f1", 2, values) }
    }

    @Test
    fun `propagates repository failure`() = runTest {
        coEvery { draftRepo.saveDraft(any(), any(), any()) } returns DomainResult.Failure(DomainError.Storage())

        val result = useCase("f1", 0, emptyMap())

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Storage)
    }
}
```

**Step 5: Replace EnqueueSubmissionUseCaseTest.kt**

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnqueueSubmissionUseCaseTest {

    private val submissionQueueRepo = mockk<SubmissionQueueRepository>()
    private val draftRepo = mockk<DraftRepository>()
    private val workScheduler = mockk<SyncWorkScheduler>(relaxUnitFun = true)
    private val useCase = EnqueueSubmissionUseCase(submissionQueueRepo, draftRepo, workScheduler)

    @Test
    fun `enqueues to repository and returns submission ID`() = runTest {
        coEvery { submissionQueueRepo.enqueue("f1", "Test Form", any()) } returns DomainResult.Success("uuid-123")
        coEvery { draftRepo.deleteDraft("f1") } returns DomainResult.Success(Unit)

        val result = useCase("f1", "Test Form", mapOf("name" to "Jane"))

        assertTrue(result is DomainResult.Success)
        assertEquals("uuid-123", (result as DomainResult.Success).data)
        coVerify { submissionQueueRepo.enqueue("f1", "Test Form", mapOf("name" to "Jane")) }
    }

    @Test
    fun `deletes draft after enqueuing`() = runTest {
        coEvery { submissionQueueRepo.enqueue(any(), any(), any()) } returns DomainResult.Success("uuid-123")
        coEvery { draftRepo.deleteDraft("f1") } returns DomainResult.Success(Unit)

        useCase("f1", "Test Form", mapOf("name" to "Jane"))

        coVerify { draftRepo.deleteDraft("f1") }
    }

    @Test
    fun `schedules sync worker after enqueuing`() = runTest {
        coEvery { submissionQueueRepo.enqueue(any(), any(), any()) } returns DomainResult.Success("uuid-123")
        coEvery { draftRepo.deleteDraft("f1") } returns DomainResult.Success(Unit)

        useCase("f1", "Test Form", mapOf("name" to "Jane"))

        verify { workScheduler.scheduleSync() }
    }

    @Test
    fun `enqueue failure propagates without deleting draft or scheduling`() = runTest {
        coEvery { submissionQueueRepo.enqueue(any(), any(), any()) } returns DomainResult.Failure(DomainError.Storage())

        val result = useCase("f1", "Test Form", mapOf("name" to "Jane"))

        assertTrue(result is DomainResult.Failure)
        coVerify(exactly = 0) { draftRepo.deleteDraft(any()) }
        verify(exactly = 0) { workScheduler.scheduleSync() }
    }
}
```

**Step 6: Run use case tests**

Run: `JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew :core:domain:test`
Expected: All tests pass

**Step 7: Commit**

```bash
git add core/domain/src/test/kotlin/com/lfr/dynamicforms/domain/usecase/GetFormUseCaseTest.kt \
       core/domain/src/test/kotlin/com/lfr/dynamicforms/domain/usecase/SubmitFormUseCaseTest.kt \
       core/domain/src/test/kotlin/com/lfr/dynamicforms/domain/usecase/SyncSubmissionUseCaseTest.kt \
       core/domain/src/test/kotlin/com/lfr/dynamicforms/domain/usecase/SaveDraftUseCaseTest.kt \
       core/domain/src/test/kotlin/com/lfr/dynamicforms/domain/usecase/EnqueueSubmissionUseCaseTest.kt
git commit -m "test: update use case tests for DomainResult and DomainError"
```

---

### Task 12: Update ViewModel tests

**Files:**
- Modify: `feature/form-wizard/src/test/java/com/lfr/dynamicforms/presentation/form/FormViewModelTest.kt`
- Modify: `feature/form-list/src/test/java/com/lfr/dynamicforms/presentation/list/FormListViewModelTest.kt`

**Step 1: Update FormViewModelTest.kt**

Key changes (not a full rewrite — only the lines that reference old types):

- `DomainResult.Failure(RuntimeException("Oops"))` → `DomainResult.Failure(DomainError.Unknown(RuntimeException("Oops")))`
- Mock `enqueueSubmission` to return `DomainResult.Success("uuid-123")` instead of raw `"uuid-123"`
- Mock `enqueueSubmission` throwing → return `DomainResult.Failure(DomainError.Storage())`
- Mock `saveDraft` to return `DomainResult.Success(Unit)` (it's `relaxUnitFun = true` currently; need to set up coEvery)
- Add import for `DomainError`

In the `loadedViewModel` helper, the `coEvery` line stays the same since `getForm` still returns `DomainResult.Success(FormWithDraft(...))`.

The full test file changes needed:

```kotlin
// Add import:
import com.lfr.dynamicforms.domain.model.DomainError

// Change LoadForm error test:
@Test
fun `LoadForm error sets errorMessage`() = runTest {
    coEvery { getForm("f1") } returns DomainResult.Failure(DomainError.Unknown(RuntimeException("Oops")))

    val vm = createViewModel()
    vm.onAction(FormAction.LoadForm("f1"))

    val state = vm.state.value
    assertFalse(state.isLoading)
    assertEquals("Something went wrong. Please try again.", state.errorMessage)
}

// Change Submit tests to use DomainResult:
@Test
fun `Submit enqueues and emits NavigateToSuccess`() = runTest {
    val vm = loadedViewModel()
    every { validatePage.validateAllPages(any(), any()) } returns emptyMap()
    coEvery { enqueueSubmission(any(), any(), any()) } returns DomainResult.Success("uuid-123")

    vm.effect.test {
        vm.onAction(FormAction.Submit)
        val effect = awaitItem()
        assertTrue(effect is FormEffect.NavigateToSuccess)
        assertEquals("Form queued for submission", (effect as FormEffect.NavigateToSuccess).message)
        cancelAndIgnoreRemainingEvents()
    }
}

@Test
fun `Submit calls EnqueueSubmissionUseCase`() = runTest {
    val vm = loadedViewModel()
    every { validatePage.validateAllPages(any(), any()) } returns emptyMap()
    coEvery { enqueueSubmission(any(), any(), any()) } returns DomainResult.Success("uuid-123")

    vm.effect.test {
        vm.onAction(FormAction.Submit)
        awaitItem()
        cancelAndIgnoreRemainingEvents()
    }

    coVerify { enqueueSubmission("f1", "Test", any()) }
}

@Test
fun `Submit sets isSubmitting false after completion`() = runTest {
    val vm = loadedViewModel()
    every { validatePage.validateAllPages(any(), any()) } returns emptyMap()
    coEvery { enqueueSubmission(any(), any(), any()) } returns DomainResult.Success("uuid-123")

    vm.effect.test {
        vm.onAction(FormAction.Submit)
        awaitItem()
        cancelAndIgnoreRemainingEvents()
    }

    assertFalse(vm.state.value.isSubmitting)
}

@Test
fun `Submit enqueue error emits ShowError`() = runTest {
    val vm = loadedViewModel()
    every { validatePage.validateAllPages(any(), any()) } returns emptyMap()
    coEvery { enqueueSubmission(any(), any(), any()) } returns DomainResult.Failure(DomainError.Storage())

    vm.effect.test {
        vm.onAction(FormAction.Submit)
        val effect = awaitItem()
        assertTrue(effect is FormEffect.ShowError)
        cancelAndIgnoreRemainingEvents()
    }
}
```

Also update the `saveDraft` mock to return `DomainResult.Success(Unit)`:

```kotlin
private val saveDraft = mockk<SaveDraftUseCase>()

// In createViewModel or init block, add:
// coEvery { saveDraft(any(), any(), any()) } returns DomainResult.Success(Unit)
```

Since `saveDraft` is used in `nextPage()`, `prevPage()`, `onCleared()`, and the debounced save, update the mock setup. The simplest approach: change from `relaxUnitFun = true` to explicit coEvery in the class init:

```kotlin
private val saveDraft = mockk<SaveDraftUseCase>()

init {
    coEvery { saveDraft(any(), any(), any()) } returns DomainResult.Success(Unit)
}
```

**Step 2: Update FormListViewModelTest.kt**

Key changes:

- Mock `formRepo.getForms()` to return `DomainResult.Success(forms)` instead of raw `forms`
- Mock `draftRepo.getFormIdsWithDrafts()` to return `DomainResult.Success(listOf(...))` instead of raw list
- Error test: return `DomainResult.Failure(DomainError.Unknown())` instead of throwing
- Mock `submissionQueueRepo.retry()` and `delete()` to return `DomainResult.Success(Unit)`

```kotlin
// Add imports:
import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult

// Update init block:
init {
    coEvery { submissionQueueRepo.observeAll() } returns pendingFlow
    coEvery { submissionQueueRepo.retry(any()) } returns DomainResult.Success(Unit)
    coEvery { submissionQueueRepo.delete(any()) } returns DomainResult.Success(Unit)
}

// Update each test's mock setup. Example:
@Test
fun `initial load populates forms and drafts`() = runTest {
    val forms = listOf(
        FormSummary("f1", "Form 1", "Desc 1", pageCount = 2, fieldCount = 5),
        FormSummary("f2", "Form 2", "Desc 2", pageCount = 3, fieldCount = 8)
    )
    coEvery { formRepo.getForms() } returns DomainResult.Success(forms)
    coEvery { draftRepo.getFormIdsWithDrafts() } returns DomainResult.Success(listOf("f1"))

    val vm = createViewModel()

    val state = vm.state.value
    assertFalse(state.isLoading)
    assertEquals(forms, state.forms)
    assertEquals(setOf("f1"), state.drafts)
    assertNull(state.errorMessage)
}

@Test
fun `initial load error sets errorMessage`() = runTest {
    coEvery { formRepo.getForms() } returns DomainResult.Failure(DomainError.Unknown())

    val vm = createViewModel()

    val state = vm.state.value
    assertFalse(state.isLoading)
    assertEquals("Something went wrong. Please try again.", state.errorMessage)
    assertTrue(state.forms.isEmpty())
}
```

Apply the same pattern to all other tests in the file.

**Step 3: Run all tests**

Run: `JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew test`
Expected: All tests pass

**Step 4: Commit**

```bash
git add feature/form-wizard/src/test/java/com/lfr/dynamicforms/presentation/form/FormViewModelTest.kt \
       feature/form-list/src/test/java/com/lfr/dynamicforms/presentation/list/FormListViewModelTest.kt
git commit -m "test: update ViewModel tests for DomainResult and DomainError"
```

---

### Task 13: Final build verification and cleanup

**Step 1: Run full build**

Run: `JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Run all tests**

Run: `JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew test`
Expected: All tests pass

**Step 3: Run detekt**

Run: `JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.12/libexec/openjdk.jdk/Contents/Home ./gradlew detekt`
Expected: No new violations

**Step 4: Verify no stale references to SubmitResult**

Search for `SubmitResult` across the codebase — should return zero matches.

**Step 5: Final commit if any cleanup was needed**

```bash
git add -A
git commit -m "chore: final cleanup after error handling unification"
```
