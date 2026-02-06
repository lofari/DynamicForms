package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.LabelElement
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme

@Composable
fun DynamicLabel(
    element: LabelElement,
    modifier: Modifier = Modifier
) {
    Text(
        text = element.text.ifBlank { element.label },
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.fillMaxWidth()
    )
}

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun DynamicLabelPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        DynamicLabel(
            element = LabelElement(id = "lbl1", label = "Label", text = "Please review all information before submitting."),
            modifier = Modifier.padding(16.dp)
        )
    }
}
