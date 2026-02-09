package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.SubmissionStatus
import com.lfr.dynamicforms.domain.repository.FormRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import javax.inject.Inject

class SyncSubmissionUseCase @Inject constructor(
    private val submissionQueueRepository: SubmissionQueueRepository,
    private val formRepository: FormRepository
) {
    suspend operator fun invoke(submission: PendingSubmission): SyncResult {
        submissionQueueRepository.updateStatus(submission.id, SubmissionStatus.SYNCING)

        val response = try {
            formRepository.submitForm(submission.formId, submission.values, idempotencyKey = submission.id)
        } catch (e: Exception) {
            val newAttemptCount = submission.attemptCount + 1
            if (newAttemptCount >= MAX_ATTEMPTS) {
                submissionQueueRepository.updateStatus(
                    submission.id,
                    SubmissionStatus.FAILED,
                    errorMessage = e.message ?: "Network error",
                    attemptCount = newAttemptCount
                )
                return SyncResult.Failed
            }
            submissionQueueRepository.updateStatus(
                submission.id,
                SubmissionStatus.PENDING,
                attemptCount = newAttemptCount
            )
            return SyncResult.Retryable
        }

        return if (response.success) {
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
    }

    companion object {
        const val MAX_ATTEMPTS = 5
    }
}

sealed class SyncResult {
    data object Synced : SyncResult()
    data object Failed : SyncResult()
    data object Retryable : SyncResult()
}
