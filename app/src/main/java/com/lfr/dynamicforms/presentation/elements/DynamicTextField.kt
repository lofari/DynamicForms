package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.TextFieldElement
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme

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
            label = { Text(requiredLabel(element.label, element.required)) },
            isError = error != null,
            minLines = if (element.multiline) 3 else 1,
            maxLines = if (element.multiline) 6 else 1,
            modifier = Modifier.fillMaxWidth()
        )
        ErrorText(error)
    }
}

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun DynamicTextFieldPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        Column(Modifier.padding(16.dp)) {
            DynamicTextField(
                element = TextFieldElement(id = "name", label = "Full Name", required = true),
                value = "Jane Doe",
                error = null,
                onValueChange = {}
            )
        }
    }
}
