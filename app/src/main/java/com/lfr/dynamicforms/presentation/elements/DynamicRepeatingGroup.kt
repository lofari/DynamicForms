package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.RepeatingGroupElement

@Composable
fun DynamicRepeatingGroup(
    element: RepeatingGroupElement,
    values: Map<String, String>,
    errors: Map<String, String>,
    itemCount: Int,
    onValueChange: (String, String) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(element.label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        for (row in 0 until itemCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Item ${row + 1}", style = MaterialTheme.typography.labelLarge)
                        if (itemCount > element.minItems) {
                            IconButton(onClick = { onRemoveRow(row) }) {
                                Icon(Icons.Default.Delete, "Remove")
                            }
                        }
                    }
                    element.elements.forEach { childElement ->
                        val key = "${element.id}[$row].${childElement.id}"
                        FormElementRenderer(
                            element = childElement,
                            value = values[key] ?: "",
                            error = errors[key],
                            values = values,
                            errors = errors,
                            onValueChange = { onValueChange(key, it) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (itemCount < element.maxItems) {
            OutlinedButton(onClick = onAddRow, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Add ${element.label}")
            }
        }
    }
}
