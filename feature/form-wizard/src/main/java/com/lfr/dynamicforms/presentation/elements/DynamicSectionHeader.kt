package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.SectionHeaderElement
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme

@Composable
fun DynamicSectionHeader(
    element: SectionHeaderElement,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(element.label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        val sub = element.subtitle
        if (!sub.isNullOrBlank()) {
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun DynamicSectionHeaderPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        DynamicSectionHeader(
            element = SectionHeaderElement(
                id = "sh1", label = "Personal Information",
                subtitle = "Please provide your contact details"
            )
        )
    }
}
