package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.DatePickerElement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicDatePicker(
    element: DatePickerElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(if (element.required) "${element.label} *" else element.label) },
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also {
                LaunchedEffect(it) {
                    it.interactions.collect { interaction ->
                        if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                            showDialog = true
                        }
                    }
                }
            }
        )
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatted = SimpleDateFormat(element.format, Locale.getDefault()).format(Date(millis))
                        onValueChange(formatted)
                    }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
