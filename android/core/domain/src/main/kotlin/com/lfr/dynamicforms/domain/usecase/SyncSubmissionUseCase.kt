package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
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
