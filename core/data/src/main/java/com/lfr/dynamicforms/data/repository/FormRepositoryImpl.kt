package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.remote.FormApi
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormSubmission
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import com.lfr.dynamicforms.domain.repository.FormRepository
import timber.log.Timber
import javax.inject.Inject

class FormRepositoryImpl @Inject constructor(
    private val api: FormApi
) : FormRepository {

    override suspend fun getForms(): List<FormSummary> {
        Timber.d("Fetching form list")
        return api.getForms()
    }

    override suspend fun getForm(formId: String): Form {
        Timber.d("Fetching form: %s", formId)
        return api.getForm(formId)
    }

    override suspend fun submitForm(
        formId: String,
        values: Map<String, String>,
        idempotencyKey: String?,
    ): SubmissionResponse {
        Timber.d("Submitting form: %s (key=%s)", formId, idempotencyKey)
        return api.submitForm(formId, FormSubmission(formId, values), idempotencyKey)
    }
}
