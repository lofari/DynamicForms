package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.*
import javax.inject.Inject

class ValidatePageUseCase @Inject constructor(
    private val evaluateVisibility: EvaluateVisibilityUseCase
) {

    fun validate(page: Page, values: Map<String, String>): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        val visibleElements = evaluateVisibility.getVisibleElements(page, values)

        for (element in visibleElements) {
            val value = values[element.id] ?: ""
            val error = validateElement(element, value)
            if (error != null) {
                errors[element.id] = error
            }
        }
        return errors
    }

    fun validateAllPages(pages: List<Page>, values: Map<String, String>): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        for (page in pages) {
            errors.putAll(validate(page, values))
        }
        return errors
    }

    fun firstPageWithErrors(pages: List<Page>, errors: Map<String, String>): Int {
        for ((index, page) in pages.withIndex()) {
            val pageFieldIds = page.elements.map { it.id }.toSet()
            if (errors.keys.any { it in pageFieldIds }) return index
        }
        return 0
    }

    private fun validateElement(element: FormElement, value: String): String? {
        if (element.required && value.isBlank()) {
            return ValidationMessages.required(element.label)
        }
        if (value.isBlank()) return null

        return when (element) {
            is TextFieldElement -> validateText(value, element.validation)
            is NumberFieldElement -> validateNumber(value, element.validation)
            is MultiSelectElement -> validateSelection(value, element.validation)
            else -> null
        }
    }

    private fun validateText(value: String, validation: TextValidation?): String? {
        validation ?: return null
        val minLen = validation.minLength
        if (minLen != null && value.length < minLen) {
            return validation.errorMessage ?: ValidationMessages.minLength(minLen)
        }
        val maxLen = validation.maxLength
        if (maxLen != null && value.length > maxLen) {
            return validation.errorMessage ?: ValidationMessages.maxLength(maxLen)
        }
        val pat = validation.pattern
        if (pat != null) {
            val matches = try {
                Regex(pat).matches(value)
            } catch (_: Exception) {
                true // treat invalid pattern as no constraint
            }
            if (!matches) return validation.errorMessage ?: ValidationMessages.INVALID_FORMAT
        }
        return null
    }

    private fun validateNumber(value: String, validation: NumberValidation?): String? {
        val number = value.toDoubleOrNull() ?: return ValidationMessages.MUST_BE_NUMBER
        validation ?: return null
        val min = validation.min
        if (min != null && number < min) {
            return validation.errorMessage ?: ValidationMessages.minValue(min)
        }
        val max = validation.max
        if (max != null && number > max) {
            return validation.errorMessage ?: ValidationMessages.maxValue(max)
        }
        return null
    }

    private fun validateSelection(value: String, validation: SelectionValidation?): String? {
        validation ?: return null
        val selections = if (value.isBlank()) 0 else value.split(",").size
        val minSel = validation.minSelections
        if (minSel != null && selections < minSel) {
            return validation.errorMessage ?: ValidationMessages.minSelections(minSel)
        }
        val maxSel = validation.maxSelections
        if (maxSel != null && selections > maxSel) {
            return validation.errorMessage ?: ValidationMessages.maxSelections(maxSel)
        }
        return null
    }
}
