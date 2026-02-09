package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import com.lfr.dynamicforms.domain.model.flatMap
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
    ): DomainResult<SubmissionResponse> {
        val errors = validatePage.validateAllPages(form.pages, values)
        if (errors.isNotEmpty()) {
            return DomainResult.Failure(DomainError.Validation(errors))
        }

        return formRepository.submitForm(form.formId, values).flatMap { response ->
            if (response.success) {
                draftRepository.deleteDraft(form.formId)
                DomainResult.Success(response)
            } else {
                DomainResult.Success(response)
            }
        }
    }
}
