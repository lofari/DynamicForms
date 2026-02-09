package com.lfr.dynamicforms.domain.repository

import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.SubmissionStatus
import kotlinx.coroutines.flow.Flow

interface SubmissionQueueRepository {
    suspend fun enqueue(formId: String, formTitle: String, values: Map<String, String>): String
    fun observeAll(): Flow<List<PendingSubmission>>
    fun observePendingCountByFormId(formId: String): Flow<Int>
    suspend fun getPendingSubmissions(): List<PendingSubmission>
    suspend fun updateStatus(id: String, status: SubmissionStatus, errorMessage: String? = null, attemptCount: Int? = null)
    suspend fun delete(id: String)
    suspend fun retry(id: String)
}
