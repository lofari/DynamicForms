package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.remote.FormApi
import com.lfr.dynamicforms.domain.model.DomainResult
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

    override suspend fun getForms(): DomainResult<List<FormSummary>> = safeCall {
        Timber.d("Fetching form list")
        api.getForms()
    }

    override suspend fun getForm(formId: String): DomainResult<Form> = safeCall {
        Timber.d("Fetching form: %s", formId)
        api.getForm(formId)
    }

    override suspend fun submitForm(
        formId: String,
        values: Map<String, String>,
        idempotencyKey: String?,
    ): DomainResult<SubmissionResponse> = safeCall {
        Timber.d("Submitting form: %s (key=%s)", formId, idempotencyKey)
        api.submitForm(formId, FormSubmission(formId, values), idempotencyKey)
    }
}
