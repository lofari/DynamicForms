package com.lfr.dynamicforms.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

// --- Validation models ---

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

// --- Visibility ---

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

// --- FormElement sealed hierarchy ---

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

// --- Form structure ---

@Serializable
data class FormDefinition(
    val formId: String,
    val title: String,
    val description: String = "",
    val pages: List<Page>
)

@Serializable
data class Page(
    val pageId: String,
    val title: String,
    val elements: List<FormElement>
)

@Serializable
data class FormSummary(
    val formId: String,
    val title: String,
    val description: String = "",
    val pageCount: Int = 0,
    val fieldCount: Int = 0
)

@Serializable
data class FormSubmission(
    val formId: String,
    val values: Map<String, String>
)

@Serializable
data class SubmissionResponse(
    val success: Boolean,
    val message: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)
