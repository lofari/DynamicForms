package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.SignatureElement
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme

@Composable
fun DynamicSignature(
    element: SignatureElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    val currentStroke = remember { mutableStateListOf<Offset>() }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = requiredLabel(element.label, element.required),
            style = MaterialTheme.typography.bodyLarge
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentStroke.clear()
                                currentStroke.add(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentStroke.add(change.position)
                            },
                            onDragEnd = {
                                strokes.add(currentStroke.toList())
                                currentStroke.clear()
                                onValueChange("signed")
                            }
                        )
                    }
            ) {
                val strokeStyle = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                fun drawPoints(points: List<Offset>) {
                    if (points.size < 2) return
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(path, Color.Black, style = strokeStyle)
                }

                strokes.forEach { drawPoints(it) }
                if (currentStroke.isNotEmpty()) {
                    drawPoints(currentStroke.toList())
                }
            }

            if (value.isBlank() && strokes.isEmpty()) {
                Text("Draw your signature here", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value.isNotBlank()) {
            TextButton(onClick = {
                strokes.clear()
                currentStroke.clear()
                onValueChange("")
            }) { Text("Clear") }
        }
        ErrorText(error)
    }
}

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun DynamicSignaturePreview() {
    DynamicFormsTheme(dynamicColor = false) {
        Column(Modifier.padding(16.dp)) {
            DynamicSignature(
                element = SignatureElement(id = "sig", label = "Signature", required = true),
                value = "",
                error = null,
                onValueChange = {}
            )
        }
    }
}
