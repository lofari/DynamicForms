package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.flatMap
import com.lfr.dynamicforms.domain.model.getDefaultValue
import com.lfr.dynamicforms.domain.model.map
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
    suspend operator fun invoke(formId: String): DomainResult<FormWithDraft> =
        formRepository.getForm(formId).flatMap { form ->
            val defaults = mutableMapOf<String, String>()
            for (page in form.pages) {
                for (element in page.elements) {
                    element.getDefaultValue()?.let { defaults[element.id] = it }
                }
            }

            draftRepository.getDraft(formId).map { draft ->
                if (draft != null) {
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
            }
        }
}
