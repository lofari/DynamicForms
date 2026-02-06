package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.CheckboxElement
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme

@Composable
fun DynamicCheckbox(
    element: CheckboxElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = value.toBooleanStrictOrNull() ?: false,
                onCheckedChange = { onValueChange(it.toString()) }
            )
            Text(requiredLabel(element.label, element.required))
        }
        ErrorText(error)
    }
}

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun DynamicCheckboxPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        Column(Modifier.padding(16.dp)) {
            DynamicCheckbox(
                element = CheckboxElement(id = "agree", label = "I agree to the terms", required = true),
                value = "true",
                error = null,
                onValueChange = {}
            )
        }
    }
}
