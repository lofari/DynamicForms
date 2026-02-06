package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.SectionHeaderElement

@Composable
fun DynamicSectionHeader(
    element: SectionHeaderElement,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(element.label, style = MaterialTheme.typography.titleLarge)
        if (!element.subtitle.isNullOrBlank()) {
            Text(element.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
