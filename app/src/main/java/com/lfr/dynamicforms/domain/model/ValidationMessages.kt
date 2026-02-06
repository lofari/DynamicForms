package com.lfr.dynamicforms.domain.model

/**
 * Default validation error messages used when the form definition
 * does not provide a custom [TextValidation.errorMessage].
 */
object ValidationMessages {
    fun required(label: String) = "$label is required"
    fun minLength(min: Int) = "Minimum $min characters"
    fun maxLength(max: Int) = "Maximum $max characters"
    const val INVALID_FORMAT = "Invalid format"
    const val MUST_BE_NUMBER = "Must be a number"
    fun minValue(min: Double) = "Minimum value is $min"
    fun maxValue(max: Double) = "Maximum value is $max"
    fun minSelections(min: Int) = "Select at least $min"
    fun maxSelections(max: Int) = "Select at most $max"
}
