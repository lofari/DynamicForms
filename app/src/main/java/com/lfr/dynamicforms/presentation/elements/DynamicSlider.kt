package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.SliderElement
import kotlin.math.roundToInt

@Composable
fun DynamicSlider(
    element: SliderElement,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentValue = value.toFloatOrNull() ?: element.min
    val steps = if (element.step > 0) ((element.max - element.min) / element.step).roundToInt() - 1 else 0

    Column(modifier = modifier.fillMaxWidth()) {
        Text("${element.label}: ${currentValue.roundToInt()}", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = currentValue,
            onValueChange = { onValueChange(it.toString()) },
            valueRange = element.min..element.max,
            steps = steps.coerceAtLeast(0)
        )
    }
}
