package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.MultiSelectElement
import com.lfr.dynamicforms.domain.model.SelectOption
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme

@Composable
fun DynamicMultiSelect(
    element: MultiSelectElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = if (value.isBlank()) emptySet() else value.split(",").toSet()

    Column(modifier = modifier.fillMaxWidth().testTag("field_${element.id}")) {
        Text(
            text = requiredLabel(element.label, element.required),
            style = MaterialTheme.typography.bodyLarge
        )
        element.options.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = option.value in selected,
                    onCheckedChange = { checked ->
                        val updated = if (checked) selected + option.value else selected - option.value
                        onValueChange(updated.joinToString(","))
                    }
                )
                Text(option.label)
            }
        }
        ErrorText(error)
    }
}

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun DynamicMultiSelectPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        Column(Modifier.padding(16.dp)) {
            DynamicMultiSelect(
                element = MultiSelectElement(
                    id = "skills", label = "Skills",
                    options = listOf(SelectOption("kt", "Kotlin"), SelectOption("java", "Java"), SelectOption("py", "Python"))
                ),
                value = "kt,py",
                error = null,
                onValueChange = {}
            )
        }
    }
}
