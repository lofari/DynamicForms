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
        if (validation.minLength != null && value.length < validation.minLength) {
            return validation.errorMessage ?: ValidationMessages.minLength(validation.minLength)
        }
        if (validation.maxLength != null && value.length > validation.maxLength) {
            return validation.errorMessage ?: ValidationMessages.maxLength(validation.maxLength)
        }
        if (validation.pattern != null) {
            val matches = try {
                Regex(validation.pattern).matches(value)
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
        if (validation.min != null && number < validation.min) {
            return validation.errorMessage ?: ValidationMessages.minValue(validation.min)
        }
        if (validation.max != null && number > validation.max) {
            return validation.errorMessage ?: ValidationMessages.maxValue(validation.max)
        }
        return null
    }

    private fun validateSelection(value: String, validation: SelectionValidation?): String? {
        validation ?: return null
        val selections = if (value.isBlank()) 0 else value.split(",").size
        if (validation.minSelections != null && selections < validation.minSelections) {
            return validation.errorMessage ?: ValidationMessages.minSelections(validation.minSelections)
        }
        if (validation.maxSelections != null && selections > validation.maxSelections) {
            return validation.errorMessage ?: ValidationMessages.maxSelections(validation.maxSelections)
        }
        return null
    }
}
