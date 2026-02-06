package com.lfr.dynamicforms.domain.repository

import com.lfr.dynamicforms.domain.model.Draft

interface DraftRepository {
    suspend fun saveDraft(formId: String, pageIndex: Int, values: Map<String, String>)
    suspend fun getDraft(formId: String): Draft?
    suspend fun deleteDraft(formId: String)
    suspend fun getFormIdsWithDrafts(): List<String>
}
