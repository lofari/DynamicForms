package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.lfr.dynamicforms.feature.formwizard.R
import com.lfr.dynamicforms.domain.model.DatePickerElement
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme
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
    val tapToPickDescription = stringResource(R.string.date_picker_tap_to_pick, element.label)

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(requiredLabel(element.label, element.required)) },
            isError = error != null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_${element.id}")
                .semantics { contentDescription = tapToPickDescription },
            interactionSource = remember { MutableInteractionSource() }.also {
                LaunchedEffect(it) {
                    it.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Release) {
                            showDialog = true
                        }
                    }
                }
            }
        )
        ErrorText(error)
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
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(group = "Form Elements", showBackground = true)
@Composable
private fun DynamicDatePickerPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        Column(Modifier.padding(16.dp)) {
            DynamicDatePicker(
                element = DatePickerElement(id = "start", label = "Start Date", required = true),
                value = "2026-01-15",
                error = null,
                onValueChange = {}
            )
        }
    }
}
