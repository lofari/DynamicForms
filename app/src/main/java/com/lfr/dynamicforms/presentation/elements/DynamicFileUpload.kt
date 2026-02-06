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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.lfr.dynamicforms.R
import com.lfr.dynamicforms.domain.model.FileUploadElement
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme

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
        val chooseFileDescription = stringResource(R.string.choose_file_content_description, element.label)
        OutlinedButton(
            onClick = { launcher.launch("*/*") },
            modifier = Modifier
                .testTag("file_upload_${element.id}")
                .semantics { contentDescription = chooseFileDescription }
        ) {
            Text(if (value.isBlank()) stringResource(R.string.choose_file) else value)
        }
        if (element.allowedTypes.isNotEmpty()) {
            Text(
                stringResource(R.string.allowed_types, element.allowedTypes.joinToString(", ")),
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
