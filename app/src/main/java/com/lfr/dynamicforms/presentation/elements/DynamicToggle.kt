package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.ToggleElement
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme

@Composable
fun DynamicToggle(
    element: ToggleElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(requiredLabel(element.label, element.required), modifier = Modifier.weight(1f))
            Switch(
                checked = value.toBooleanStrictOrNull() ?: element.defaultValue,
                onCheckedChange = { onValueChange(it.toString()) },
                modifier = Modifier.testTag("field_${element.id}")
            )
        }
        ErrorText(error)
    }
}

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun DynamicTogglePreview() {
    DynamicFormsTheme(dynamicColor = false) {
        Column(Modifier.padding(16.dp)) {
            DynamicToggle(
                element = ToggleElement(id = "notify", label = "Enable notifications"),
                value = "true",
                error = null,
                onValueChange = {}
            )
        }
    }
}
