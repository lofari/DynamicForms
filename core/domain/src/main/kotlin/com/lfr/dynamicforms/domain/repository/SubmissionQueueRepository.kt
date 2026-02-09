package com.lfr.dynamicforms.domain.repository

import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.SubmissionStatus
import kotlinx.coroutines.flow.Flow

interface SubmissionQueueRepository {
    suspend fun enqueue(formId: String, formTitle: String, values: Map<String, String>): DomainResult<String>
    fun observeAll(): Flow<List<PendingSubmission>>
    fun observePendingCountByFormId(formId: String): Flow<Int>
    suspend fun getPendingSubmissions(): DomainResult<List<PendingSubmission>>
    suspend fun updateStatus(id: String, status: SubmissionStatus, errorMessage: String? = null, attemptCount: Int? = null): DomainResult<Unit>
    suspend fun delete(id: String): DomainResult<Unit>
    suspend fun retry(id: String): DomainResult<Unit>
}
