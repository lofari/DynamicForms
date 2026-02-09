package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.local.PendingSubmissionDao
import com.lfr.dynamicforms.data.local.PendingSubmissionEntity
import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.SubmissionStatus
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

class SubmissionQueueRepositoryImpl @Inject constructor(
    private val dao: PendingSubmissionDao,
    private val json: Json
) : SubmissionQueueRepository {

    override suspend fun enqueue(formId: String, formTitle: String, values: Map<String, String>): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.upsert(
            PendingSubmissionEntity(
                id = id,
                formId = formId,
                formTitle = formTitle,
                valuesJson = json.encodeToString(values),
                status = SubmissionStatus.PENDING.name,
                errorMessage = null,
                attemptCount = 0,
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    override fun observeAll(): Flow<List<PendingSubmission>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observePendingCountByFormId(formId: String): Flow<Int> =
        dao.observePendingCountByFormId(formId)

    override suspend fun getPendingSubmissions(): List<PendingSubmission> =
        dao.getPending().map { it.toDomain() }

    override suspend fun updateStatus(
        id: String,
        status: SubmissionStatus,
        errorMessage: String?,
        attemptCount: Int?
    ) {
        val existing = dao.getPending().find { it.id == id }
        dao.updateStatus(
            id = id,
            status = status.name,
            errorMessage = errorMessage,
            attemptCount = attemptCount ?: existing?.attemptCount ?: 0,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }

    override suspend fun retry(id: String) {
        dao.updateStatus(
            id = id,
            status = SubmissionStatus.PENDING.name,
            errorMessage = null,
            attemptCount = 0,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun PendingSubmissionEntity.toDomain(): PendingSubmission = PendingSubmission(
        id = id,
        formId = formId,
        formTitle = formTitle,
        values = try {
            json.decodeFromString(valuesJson)
        } catch (_: Exception) {
            emptyMap()
        },
        status = try {
            SubmissionStatus.valueOf(status)
        } catch (_: Exception) {
            SubmissionStatus.PENDING
        },
        errorMessage = errorMessage,
        attemptCount = attemptCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
