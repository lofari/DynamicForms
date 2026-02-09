package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.local.DraftDao
import com.lfr.dynamicforms.data.local.DraftEntity
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Draft
import com.lfr.dynamicforms.domain.repository.DraftRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DraftRepositoryImpl @Inject constructor(
    private val draftDao: DraftDao,
    private val json: Json
) : DraftRepository {

    override suspend fun saveDraft(formId: String, pageIndex: Int, values: Map<String, String>): DomainResult<Unit> =
        safeStorageCall {
            draftDao.upsert(
                DraftEntity(
                    formId = formId,
                    pageIndex = pageIndex,
                    valuesJson = json.encodeToString(values),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

    override suspend fun getDraft(formId: String): DomainResult<Draft?> = safeStorageCall {
        val entity = draftDao.getDraft(formId) ?: return@safeStorageCall null
        val values: Map<String, String> = try {
            json.decodeFromString(entity.valuesJson)
        } catch (_: Exception) {
            draftDao.deleteDraft(formId)
            return@safeStorageCall null
        }
        Draft(
            formId = entity.formId,
            pageIndex = entity.pageIndex,
            values = values,
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun deleteDraft(formId: String): DomainResult<Unit> = safeStorageCall {
        draftDao.deleteDraft(formId)
    }

    override suspend fun getFormIdsWithDrafts(): DomainResult<List<String>> = safeStorageCall {
        draftDao.getFormIdsWithDrafts()
    }
}
