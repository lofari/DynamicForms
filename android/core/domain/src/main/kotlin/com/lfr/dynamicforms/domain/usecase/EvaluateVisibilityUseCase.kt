package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.ConditionOperator
import com.lfr.dynamicforms.domain.model.FormElement
import com.lfr.dynamicforms.domain.model.Page
import com.lfr.dynamicforms.domain.model.VisibilityCondition
import javax.inject.Inject

class EvaluateVisibilityUseCase @Inject constructor() {

    fun getVisibleElements(page: Page, values: Map<String, String>): List<FormElement> {
        return page.elements.filter { isVisible(it, values) }
    }

    fun isVisible(element: FormElement, values: Map<String, String>): Boolean {
        val condition = element.visibleWhen ?: return true
        return evaluate(condition, values)
    }

    private fun evaluate(condition: VisibilityCondition, values: Map<String, String>): Boolean {
        val fieldValue = values[condition.fieldId] ?: ""
        val condValue = condition.value.content

        return when (condition.operator) {
            ConditionOperator.EQUALS -> fieldValue == condValue
            ConditionOperator.NOT_EQUALS -> fieldValue != condValue
            ConditionOperator.GREATER_THAN -> {
                val fv = fieldValue.toDoubleOrNull() ?: return false
                val cv = condValue.toDoubleOrNull() ?: return false
                fv > cv
            }
            ConditionOperator.LESS_THAN -> {
                val fv = fieldValue.toDoubleOrNull() ?: return false
                val cv = condValue.toDoubleOrNull() ?: return false
                fv < cv
            }
            ConditionOperator.CONTAINS -> fieldValue.contains(condValue, ignoreCase = true)
            ConditionOperator.IS_EMPTY -> fieldValue.isEmpty()
            ConditionOperator.IS_NOT_EMPTY -> fieldValue.isNotEmpty()
        }
    }
}
