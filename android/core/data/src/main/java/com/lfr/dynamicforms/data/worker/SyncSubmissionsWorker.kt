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
