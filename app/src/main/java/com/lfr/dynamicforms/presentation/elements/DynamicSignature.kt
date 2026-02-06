package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.SignatureElement

@Composable
fun DynamicSignature(
    element: SignatureElement,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (value.isBlank()) {
                Text("Tap to sign", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Signed", color = MaterialTheme.colorScheme.primary)
            }
        }
        if (value.isNotBlank()) {
            TextButton(onClick = { onValueChange("") }) { Text("Clear") }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
