# Offline-First Submission Queue

## Problem

The app currently requires network connectivity to submit forms. If the user fills out a multi-page form in a poor-connectivity environment (field inspections, construction sites, remote areas), tapping Submit fails and their work is at risk. This is a common real-world mobile constraint that the app should handle gracefully.

## Solution

A local submission queue that persists completed forms to Room, syncs them to the server via WorkManager when connectivity is available, and surfaces queue status in the UI. Submissions feel instant to the user -- they never wait for the network.

## Design

### 1. Data Model & Storage

New Room entity `PendingSubmissionEntity` in the existing `AppDatabase`:

| Column | Type | Purpose |
|--------|------|---------|
| `id` | `String` (PK) | UUID, doubles as idempotency key |
| `formId` | `String` | Which form this belongs to |
| `formTitle` | `String` | Cached for display without re-fetching |
| `valuesJson` | `String` | Serialized `Map<String, String>` |
| `status` | `String` | PENDING, SYNCING, or FAILED |
| `errorMessage` | `String?` | Null unless FAILED; stores server error |
| `attemptCount` | `Int` | Tracks retries, caps at 5 |
| `createdAt` | `Long` | Creation timestamp |
| `updatedAt` | `Long` | Last status change |

`PendingSubmissionDao` queries:
- `getAll(): Flow<List<PendingSubmissionEntity>>` -- reactive, drives UI
- `getByStatus(status): List<PendingSubmissionEntity>` -- Worker finds PENDING items
- `getPendingCountByFormId(formId): Flow<Int>` -- badge on FormListScreen
- `upsert(entity)`, `delete(id)`, `updateStatus(id, status, errorMessage?)`

Domain layer gets a `PendingSubmission` data class (no Room annotations) and a `SubmissionQueueRepository` interface.

### 2. Domain Layer

**`SubmissionQueueRepository`** interface:
- `enqueue(formId, formTitle, values): String` -- creates entry, returns UUID
- `observeAll(): Flow<List<PendingSubmission>>` -- reactive stream for UI
- `observePendingCountByFormId(formId): Flow<Int>` -- badge counts
- `getPendingSubmissions(): List<PendingSubmission>` -- non-reactive, for Worker
- `updateStatus(id, status, errorMessage?)` -- Worker updates after attempt
- `delete(id)` -- user discards failed submission
- `retry(id)` -- resets to PENDING, clears error, resets attemptCount

**`EnqueueSubmissionUseCase`** replaces direct API call in submit flow:
1. Saves values to queue via repository
2. Deletes the draft (submission is safely persisted in queue)
3. Schedules `SyncSubmissionsWorker` via WorkManager
4. Returns immediately -- user sees success screen instantly

**`SyncSubmissionUseCase`** called by Worker for each pending item:
1. Updates status to SYNCING
2. Calls `formRepository.submitForm(formId, values, idempotencyKey)`
3. On success: deletes queue entry
4. On 4xx (validation/client error): marks FAILED with error message (non-retryable)
5. On 5xx/network error: increments attemptCount, stays PENDING (retryable)
6. After 5 failed attempts: marks FAILED (stops retrying)

Existing `SubmitFormUseCase` stays unchanged -- it handles client-side validation. Flow becomes: validate locally -> enqueue -> sync in background.

### 3. WorkManager Integration

**`SyncSubmissionsWorker`** -- `CoroutineWorker` with `@HiltWorker`:
- Queries all PENDING submissions
- Processes sequentially (avoids overwhelming server)
- Delegates each to `SyncSubmissionUseCase`
- Returns `Result.success()` when all processed (even if some failed)
- Returns `Result.retry()` only if queue itself couldn't be read

**Scheduling:**
- `EnqueueSubmissionUseCase` triggers `OneTimeWorkRequest` with `Constraints(requiredNetworkType = NetworkType.CONNECTED)`
- Tagged `"sync_submissions"` with `ExistingWorkPolicy.KEEP`
- WorkManager handles connectivity waiting, process death survival, Doze mode

**Retry strategy:**
- Worker runs when connectivity available and pending work exists
- Retryable items (5xx, timeout) get attemptCount incremented
- After 5 attempts, status changes to FAILED
- User can manually retry, which resets attemptCount and re-schedules Worker

**Server-side idempotency:**
- `POST /forms/{id}/submit` gains `Idempotency-Key` header
- Backend stores processed keys in a `Set<String>`
- Duplicate key returns 200 with original response

### 4. UI Changes

**FormViewModel:** `submit()` calls `EnqueueSubmissionUseCase` instead of direct API. Emits `NavigateToSuccess` immediately. No loading spinner waiting for network.

**FormListScreen additions:**

*Badges on form cards:*
- Small chip showing pending count (e.g., "2 pending", "1 failed")
- Color-coded: neutral for PENDING, red for FAILED
- Only shown when count > 0

*Pending Submissions section at top of list:*
- Collapsible section, only visible when queued items exist
- Each item shows: form title, status chip, relative timestamp
- FAILED items show error message on secondary line
- Swipe-to-dismiss on FAILED items triggers discard
- Tap on FAILED items triggers retry
- Subtle animation on SYNCING items

**No changes to FormScreen** -- form filling experience stays identical.

### 5. Testing

**New unit tests:**
- `EnqueueSubmissionUseCaseTest` -- saves to queue, deletes draft, schedules worker
- `SyncSubmissionUseCaseTest` -- all status transitions: success, 4xx, 5xx, max attempts
- `SubmissionQueueRepositoryImplTest` -- DAO interactions, serialization
- `FormListViewModelTest` -- pending counts, retry, discard actions
- `SyncSubmissionsWorkerTest` -- processes items, returns correct Result

**New UI tests:**
- `PendingSubmissionsSectionTest` -- renders status chips, swipe/tap actions, hidden when empty

**Updated tests:**
- `FormViewModelTest` -- submit calls enqueue instead of direct API
- `FormRoutesTest` (backend) -- idempotency: duplicate key returns 200

Total: ~15-20 new test cases across ~6 new test files, plus updates to 2 existing.

### 6. File Inventory

**New files (11):**

| File | Layer |
|------|-------|
| `domain/model/PendingSubmission.kt` | Domain model + Status enum |
| `domain/repository/SubmissionQueueRepository.kt` | Repository interface |
| `domain/usecase/EnqueueSubmissionUseCase.kt` | Queue + delete draft + schedule |
| `domain/usecase/SyncSubmissionUseCase.kt` | Sync one item with status transitions |
| `data/local/PendingSubmissionEntity.kt` | Room entity |
| `data/local/PendingSubmissionDao.kt` | Room DAO with Flow queries |
| `data/repository/SubmissionQueueRepositoryImpl.kt` | Repository implementation |
| `data/worker/SyncSubmissionsWorker.kt` | HiltWorker |
| `presentation/list/PendingSubmissionsSection.kt` | Queue UI composable |
| `presentation/list/FormListViewModel.kt` | Extracted ViewModel |
| `di/WorkerModule.kt` | Hilt WorkManager module |

**Modified files (7):**

| File | Change |
|------|--------|
| `data/local/AppDatabase.kt` | Add DAO, bump version |
| `data/remote/FormApi.kt` | Add idempotencyKey header |
| `data/remote/MockInterceptor.kt` | Handle idempotency |
| `di/RepositoryModule.kt` | Bind new repository |
| `presentation/form/FormViewModel.kt` | Submit calls enqueue |
| `presentation/list/FormListScreen.kt` | Badges + pending section |
| `backend/.../FormRoutes.kt` | Idempotency check |
