package com.lfr.dynamicforms.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TextValidation(
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val errorMessage: String? = null
)

@Serializable
data class NumberValidation(
    val min: Double? = null,
    val max: Double? = null,
    val errorMessage: String? = null
)

@Serializable
data class SelectionValidation(
    val minSelections: Int? = null,
    val maxSelections: Int? = null,
    val errorMessage: String? = null
)

@Serializable
data class SelectOption(
    val value: String,
    val label: String
)
