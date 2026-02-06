package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.remote.FormApi
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormSubmission
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import com.lfr.dynamicforms.domain.repository.FormRepository
import javax.inject.Inject

class FormRepositoryImpl @Inject constructor(
    private val api: FormApi
) : FormRepository {

    override suspend fun getForms(): List<FormSummary> = api.getForms()

    override suspend fun getForm(formId: String): Form = api.getForm(formId)

    override suspend fun submitForm(formId: String, values: Map<String, String>): SubmissionResponse {
        return api.submitForm(formId, FormSubmission(formId, values))
    }
}
