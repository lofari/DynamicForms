package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.TextFieldElement

@Composable
fun DynamicTextField(
    element: TextFieldElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(if (element.required) "${element.label} *" else element.label) },
            isError = error != null,
            minLines = if (element.multiline) 3 else 1,
            maxLines = if (element.multiline) 6 else 1,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
