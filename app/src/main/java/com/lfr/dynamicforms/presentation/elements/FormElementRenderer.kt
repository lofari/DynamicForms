package com.lfr.dynamicforms.presentation.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.*

@Composable
fun FormElementRenderer(
    element: FormElement,
    value: String,
    error: String?,
    values: Map<String, String>,
    errors: Map<String, String>,
    onValueChange: (String) -> Unit,
    onRepeatingGroupValueChange: (String, String) -> Unit = { _, _ -> },
    onAddRow: (String) -> Unit = {},
    onRemoveRow: (String, Int) -> Unit = { _, _ -> },
    repeatingGroupCounts: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    when (element) {
        is TextFieldElement -> DynamicTextField(element, value, error, onValueChange, modifier)
        is NumberFieldElement -> DynamicNumberField(element, value, error, onValueChange, modifier)
        is DropdownElement -> DynamicDropdown(element, value, error, onValueChange, modifier)
        is CheckboxElement -> DynamicCheckbox(element, value, error, onValueChange, modifier)
        is RadioElement -> DynamicRadioGroup(element, value, error, onValueChange, modifier)
        is DatePickerElement -> DynamicDatePicker(element, value, error, onValueChange, modifier)
        is ToggleElement -> DynamicToggle(element, value, onValueChange, modifier)
        is SliderElement -> DynamicSlider(element, value, onValueChange, modifier)
        is MultiSelectElement -> DynamicMultiSelect(element, value, error, onValueChange, modifier)
        is FileUploadElement -> DynamicFileUpload(element, value, error, onValueChange, modifier)
        is SignatureElement -> DynamicSignature(element, value, error, onValueChange, modifier)
        is SectionHeaderElement -> DynamicSectionHeader(element, modifier)
        is LabelElement -> DynamicLabel(element, modifier)
        is RepeatingGroupElement -> DynamicRepeatingGroup(
            element = element,
            values = values,
            errors = errors,
            itemCount = repeatingGroupCounts[element.id] ?: element.minItems,
            onValueChange = onRepeatingGroupValueChange,
            onAddRow = { onAddRow(element.id) },
            onRemoveRow = { row -> onRemoveRow(element.id, row) },
            modifier = modifier
        )
    }
}
