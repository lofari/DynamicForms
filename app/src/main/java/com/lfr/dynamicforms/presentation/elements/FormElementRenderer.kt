package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import com.lfr.dynamicforms.domain.model.CheckboxElement
import com.lfr.dynamicforms.domain.model.DatePickerElement
import com.lfr.dynamicforms.domain.model.DropdownElement
import com.lfr.dynamicforms.domain.model.FileUploadElement
import com.lfr.dynamicforms.domain.model.FormElement
import com.lfr.dynamicforms.domain.model.LabelElement
import com.lfr.dynamicforms.domain.model.MultiSelectElement
import com.lfr.dynamicforms.domain.model.NumberFieldElement
import com.lfr.dynamicforms.domain.model.RadioElement
import com.lfr.dynamicforms.domain.model.RepeatingGroupElement
import com.lfr.dynamicforms.domain.model.SectionHeaderElement
import com.lfr.dynamicforms.domain.model.SignatureElement
import com.lfr.dynamicforms.domain.model.SliderElement
import com.lfr.dynamicforms.domain.model.TextFieldElement
import com.lfr.dynamicforms.domain.model.ToggleElement

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
        is ToggleElement -> DynamicToggle(element, value, error, onValueChange, modifier)
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

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun FormElementRendererPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FormElementRenderer(
                element = TextFieldElement(id = "name", label = "Full Name", required = true),
                value = "Jane Doe",
                error = null,
                values = emptyMap(),
                errors = emptyMap(),
                onValueChange = {}
            )
            FormElementRenderer(
                element = CheckboxElement(id = "agree", label = "I accept the terms"),
                value = "false",
                error = null,
                values = emptyMap(),
                errors = emptyMap(),
                onValueChange = {}
            )
        }
    }
}
