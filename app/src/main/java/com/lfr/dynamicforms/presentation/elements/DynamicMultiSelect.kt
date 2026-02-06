package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.MultiSelectElement

@Composable
fun DynamicMultiSelect(
    element: MultiSelectElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = if (value.isBlank()) emptySet() else value.split(",").toSet()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (element.required) "${element.label} *" else element.label,
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
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
