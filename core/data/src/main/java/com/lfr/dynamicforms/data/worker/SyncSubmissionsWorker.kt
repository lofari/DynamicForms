package com.lfr.dynamicforms.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
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
        val pending = submissionQueueRepository.getPendingSubmissions()
        Timber.d("SyncSubmissionsWorker: %d pending submissions", pending.size)
        for (submission in pending) {
            Timber.d("Syncing submission %s for form %s", submission.id, submission.formId)
            syncSubmission(submission)
        }
        Timber.d("SyncSubmissionsWorker: completed")
        return Result.success()
    }
}
