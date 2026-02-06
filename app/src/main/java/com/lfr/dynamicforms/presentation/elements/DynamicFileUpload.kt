package com.lfr.dynamicforms.presentation.elements

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.FileUploadElement
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme

@Composable
fun DynamicFileUpload(
    element: FileUploadElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
            onValueChange(displayName ?: uri.toString())
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = requiredLabel(element.label, element.required),
            style = MaterialTheme.typography.bodyLarge
        )
        OutlinedButton(onClick = { launcher.launch("*/*") }) {
            Text(if (value.isBlank()) "Choose file" else value)
        }
        if (element.allowedTypes.isNotEmpty()) {
            Text(
                "Allowed: ${element.allowedTypes.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        ErrorText(error)
    }
}

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun DynamicFileUploadPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        Column(Modifier.padding(16.dp)) {
            DynamicFileUpload(
                element = FileUploadElement(
                    id = "doc", label = "Upload Document", required = true,
                    allowedTypes = listOf("pdf", "docx", "jpg")
                ),
                value = "",
                error = null,
                onValueChange = {}
            )
        }
    }
}
