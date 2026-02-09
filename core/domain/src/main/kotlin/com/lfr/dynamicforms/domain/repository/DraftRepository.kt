package com.lfr.dynamicforms.domain.repository

import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Draft

interface DraftRepository {
    suspend fun saveDraft(formId: String, pageIndex: Int, values: Map<String, String>): DomainResult<Unit>
    suspend fun getDraft(formId: String): DomainResult<Draft?>
    suspend fun deleteDraft(formId: String): DomainResult<Unit>
    suspend fun getFormIdsWithDrafts(): DomainResult<List<String>>
}
