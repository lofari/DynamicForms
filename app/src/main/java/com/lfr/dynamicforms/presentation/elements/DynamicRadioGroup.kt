package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.RadioElement
import com.lfr.dynamicforms.domain.model.SelectOption
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme

@Composable
fun DynamicRadioGroup(
    element: RadioElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = requiredLabel(element.label, element.required),
            style = MaterialTheme.typography.bodyLarge
        )
        element.options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = value == option.value,
                        onClick = { onValueChange(option.value) }
                    )
            ) {
                RadioButton(selected = value == option.value, onClick = { onValueChange(option.value) })
                Text(option.label)
            }
        }
        ErrorText(error)
    }
}

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun DynamicRadioGroupPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        Column(Modifier.padding(16.dp)) {
            DynamicRadioGroup(
                element = RadioElement(
                    id = "priority", label = "Priority", required = true,
                    options = listOf(SelectOption("low", "Low"), SelectOption("med", "Medium"), SelectOption("high", "High"))
                ),
                value = "med",
                error = null,
                onValueChange = {}
            )
        }
    }
}
