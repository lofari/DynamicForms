package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.LabelElement

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
