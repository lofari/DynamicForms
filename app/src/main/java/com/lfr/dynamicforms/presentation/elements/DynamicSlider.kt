package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.SliderElement
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
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

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun DynamicSliderPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        Column(Modifier.padding(16.dp)) {
            DynamicSlider(
                element = SliderElement(id = "rating", label = "Rating", min = 0f, max = 10f, step = 1f),
                value = "7",
                onValueChange = {}
            )
        }
    }
}
