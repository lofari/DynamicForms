package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme

fun requiredLabel(label: String, required: Boolean): String =
    if (required) "$label *" else label

@Composable
fun ErrorText(error: String?) {
    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorTextPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        Column(Modifier.padding(16.dp)) {
            ErrorText(error = "This field is required")
        }
    }
}
