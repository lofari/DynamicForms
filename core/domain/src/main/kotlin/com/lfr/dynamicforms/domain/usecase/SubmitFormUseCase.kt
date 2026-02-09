package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import javax.inject.Inject

class SubmitFormUseCase @Inject constructor(
    private val formRepository: FormRepository,
    private val draftRepository: DraftRepository,
    private val validatePage: ValidatePageUseCase
) {
    suspend operator fun invoke(
        form: Form,
        values: Map<String, String>
    ): SubmitResult {
        val errors = validatePage.validateAllPages(form.pages, values)
        if (errors.isNotEmpty()) {
            val firstPage = validatePage.firstPageWithErrors(form.pages, errors)
            return SubmitResult.ValidationFailed(errors, firstPage)
        }

        val response = try {
            formRepository.submitForm(form.formId, values)
        } catch (e: Exception) {
            return SubmitResult.NetworkError(e)
        }

        return if (response.success) {
            draftRepository.deleteDraft(form.formId)
            SubmitResult.Success(response.message ?: "Form submitted successfully")
        } else {
            val firstPage = validatePage.firstPageWithErrors(form.pages, response.fieldErrors)
            SubmitResult.ServerError(response.fieldErrors, firstPage)
        }
    }
}

sealed class SubmitResult {
    data class Success(val message: String) : SubmitResult()
    data class ValidationFailed(val errors: Map<String, String>, val firstErrorPage: Int) : SubmitResult()
    data class ServerError(val fieldErrors: Map<String, String>, val firstErrorPage: Int) : SubmitResult()
    data class NetworkError(val exception: Throwable) : SubmitResult()
}
