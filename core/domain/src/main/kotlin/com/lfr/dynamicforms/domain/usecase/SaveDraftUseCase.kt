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
