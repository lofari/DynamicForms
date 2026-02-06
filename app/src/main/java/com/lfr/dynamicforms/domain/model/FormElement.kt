package com.lfr.dynamicforms.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class VisibilityCondition(
    val fieldId: String,
    val operator: ConditionOperator,
    val value: JsonPrimitive
)

@Serializable
enum class ConditionOperator {
    @SerialName("equals") EQUALS,
    @SerialName("not_equals") NOT_EQUALS,
    @SerialName("greater_than") GREATER_THAN,
    @SerialName("less_than") LESS_THAN,
    @SerialName("contains") CONTAINS,
    @SerialName("is_empty") IS_EMPTY,
    @SerialName("is_not_empty") IS_NOT_EMPTY
}

@Serializable
sealed class FormElement {
    abstract val id: String
    abstract val label: String
    abstract val required: Boolean
    abstract val visibleWhen: VisibilityCondition?
}

@Serializable @SerialName("text_field")
data class TextFieldElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val multiline: Boolean = false,
    val validation: TextValidation? = null
) : FormElement()

@Serializable @SerialName("number_field")
data class NumberFieldElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val validation: NumberValidation? = null
) : FormElement()

@Serializable @SerialName("dropdown")
data class DropdownElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val options: List<SelectOption> = emptyList()
) : FormElement()

@Serializable @SerialName("checkbox")
data class CheckboxElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val defaultValue: Boolean = false
) : FormElement()

@Serializable @SerialName("radio")
data class RadioElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val options: List<SelectOption> = emptyList()
) : FormElement()

@Serializable @SerialName("date_picker")
data class DatePickerElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val minDate: String? = null,
    val maxDate: String? = null,
    val format: String = "yyyy-MM-dd"
) : FormElement()

@Serializable @SerialName("toggle")
data class ToggleElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val defaultValue: Boolean = false
) : FormElement()

@Serializable @SerialName("slider")
data class SliderElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f
) : FormElement()

@Serializable @SerialName("multi_select")
data class MultiSelectElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val options: List<SelectOption> = emptyList(),
    val validation: SelectionValidation? = null
) : FormElement()

@Serializable @SerialName("file_upload")
data class FileUploadElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val maxFileSize: Long = 10_485_760,
    val allowedTypes: List<String> = emptyList()
) : FormElement()

@Serializable @SerialName("signature")
data class SignatureElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null
) : FormElement()

@Serializable @SerialName("section_header")
data class SectionHeaderElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val subtitle: String? = null
) : FormElement()

@Serializable @SerialName("label")
data class LabelElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val text: String = ""
) : FormElement()

@Serializable @SerialName("repeating_group")
data class RepeatingGroupElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val minItems: Int = 1,
    val maxItems: Int = 10,
    val elements: List<FormElement> = emptyList()
) : FormElement()

fun FormElement.getDefaultValue(): String? = when (this) {
    is ToggleElement -> defaultValue.toString()
    is CheckboxElement -> defaultValue.toString()
    is SliderElement -> min.toString()
    else -> null
}
