package com.lfr.dynamicforms.data.remote

import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormSubmission
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface FormApi {
    @GET("forms")
    suspend fun getForms(): List<FormSummary>

    @GET("forms/{formId}")
    suspend fun getForm(@Path("formId") formId: String): Form

    @POST("forms/{formId}/submit")
    suspend fun submitForm(
        @Path("formId") formId: String,
        @Body submission: FormSubmission,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): SubmissionResponse
}
