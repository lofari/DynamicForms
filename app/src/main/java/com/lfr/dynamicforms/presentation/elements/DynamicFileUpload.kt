package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.FileUploadElement

@Composable
fun DynamicFileUpload(
    element: FileUploadElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (element.required) "${element.label} *" else element.label,
            style = MaterialTheme.typography.bodyLarge
        )
        OutlinedButton(onClick = { /* TODO: launch file picker */ }) {
            Text(if (value.isBlank()) "Choose file" else value)
        }
        if (element.allowedTypes.isNotEmpty()) {
            Text(
                "Allowed: ${element.allowedTypes.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
