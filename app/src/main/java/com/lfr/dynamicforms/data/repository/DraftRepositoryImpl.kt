package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.local.DraftDao
import com.lfr.dynamicforms.data.local.DraftEntity
import com.lfr.dynamicforms.domain.model.Draft
import com.lfr.dynamicforms.domain.repository.DraftRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DraftRepositoryImpl @Inject constructor(
    private val draftDao: DraftDao
) : DraftRepository {

    private val json = Json

    override suspend fun saveDraft(formId: String, pageIndex: Int, values: Map<String, String>) {
        draftDao.upsert(
            DraftEntity(
                formId = formId,
                pageIndex = pageIndex,
                valuesJson = json.encodeToString(values),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getDraft(formId: String): Draft? {
        val entity = draftDao.getDraft(formId) ?: return null
        return Draft(
            formId = entity.formId,
            pageIndex = entity.pageIndex,
            values = json.decodeFromString(entity.valuesJson),
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun deleteDraft(formId: String) {
        draftDao.deleteDraft(formId)
    }

    override suspend fun getFormIdsWithDrafts(): List<String> {
        return draftDao.getFormIdsWithDrafts()
    }
}
