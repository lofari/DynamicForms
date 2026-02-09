package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import javax.inject.Inject

class EnqueueSubmissionUseCase @Inject constructor(
    private val submissionQueueRepository: SubmissionQueueRepository,
    private val draftRepository: DraftRepository,
    private val workScheduler: SyncWorkScheduler
) {
    suspend operator fun invoke(formId: String, formTitle: String, values: Map<String, String>): String {
        val submissionId = submissionQueueRepository.enqueue(formId, formTitle, values)
        draftRepository.deleteDraft(formId)
        workScheduler.scheduleSync()
        return submissionId
    }
}
