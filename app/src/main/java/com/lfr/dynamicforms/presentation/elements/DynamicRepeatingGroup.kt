package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.R
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme
import com.lfr.dynamicforms.domain.model.RepeatingGroupElement
import com.lfr.dynamicforms.domain.model.TextFieldElement

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
                    .testTag("repeating_group_${element.id}_row_$row")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.repeating_item_label, row + 1), style = MaterialTheme.typography.labelLarge)
                        if (itemCount > element.minItems) {
                            IconButton(onClick = { onRemoveRow(row) }) {
                                Icon(Icons.Default.Delete, stringResource(R.string.remove_item_content_description, row + 1))
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
                Icon(Icons.Default.Add, stringResource(R.string.add_item_content_description))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add_label, element.label))
            }
        }
    }
}

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun DynamicRepeatingGroupPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        Column(Modifier.padding(16.dp)) {
            DynamicRepeatingGroup(
                element = RepeatingGroupElement(
                    id = "contacts", label = "Emergency Contacts",
                    minItems = 1, maxItems = 3,
                    elements = listOf(
                        TextFieldElement(id = "name", label = "Name", required = true),
                        TextFieldElement(id = "phone", label = "Phone"),
                    )
                ),
                values = mapOf("contacts[0].name" to "John Smith", "contacts[0].phone" to "555-0100"),
                errors = emptyMap(),
                itemCount = 2,
                onValueChange = { _, _ -> },
                onAddRow = {},
                onRemoveRow = {},
            )
        }
    }
}
