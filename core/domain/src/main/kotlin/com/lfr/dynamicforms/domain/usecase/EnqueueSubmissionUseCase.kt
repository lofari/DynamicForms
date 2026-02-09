package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainResult
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
