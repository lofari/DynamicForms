package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.ToggleElement

@Composable
fun DynamicToggle(
    element: ToggleElement,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(element.label, modifier = Modifier.weight(1f))
        Switch(
            checked = value.toBooleanStrictOrNull() ?: element.defaultValue,
            onCheckedChange = { onValueChange(it.toString()) }
        )
    }
}
