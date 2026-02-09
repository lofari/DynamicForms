package com.lfr.dynamicforms.domain.repository

import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.SubmissionResponse

interface FormRepository {
    suspend fun getForms(): DomainResult<List<FormSummary>>
    suspend fun getForm(formId: String): DomainResult<Form>
    suspend fun submitForm(formId: String, values: Map<String, String>, idempotencyKey: String? = null): DomainResult<SubmissionResponse>
}
