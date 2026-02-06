package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.getDefaultValue
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import javax.inject.Inject

data class FormWithDraft(
    val form: Form,
    val initialValues: Map<String, String>,
    val initialPageIndex: Int
)

class GetFormUseCase @Inject constructor(
    private val formRepository: FormRepository,
    private val draftRepository: DraftRepository
) {
    suspend operator fun invoke(formId: String): DomainResult<FormWithDraft> = try {
        val form = formRepository.getForm(formId)

        val defaults = mutableMapOf<String, String>()
        for (page in form.pages) {
            for (element in page.elements) {
                element.getDefaultValue()?.let { defaults[element.id] = it }
            }
        }

        val draft = draftRepository.getDraft(formId)
        val result = if (draft != null) {
            FormWithDraft(
                form = form,
                initialValues = defaults + draft.values,
                initialPageIndex = draft.pageIndex
            )
        } else {
            FormWithDraft(
                form = form,
                initialValues = defaults,
                initialPageIndex = 0
            )
        }
        DomainResult.Success(result)
    } catch (e: Exception) {
        DomainResult.Failure(e)
    }
}
