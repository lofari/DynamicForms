package com.lfr.dynamicforms.domain.repository

import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.SubmissionResponse

interface FormRepository {
    suspend fun getForms(): List<FormSummary>
    suspend fun getForm(formId: String): Form
    suspend fun submitForm(formId: String, values: Map<String, String>, idempotencyKey: String? = null): SubmissionResponse
}
