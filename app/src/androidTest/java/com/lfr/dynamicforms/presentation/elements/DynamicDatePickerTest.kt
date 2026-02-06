package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lfr.dynamicforms.domain.model.DatePickerElement
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme
import org.junit.Rule
import org.junit.Test

class DynamicDatePickerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_label_with_required_asterisk() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicDatePicker(
                    element = DatePickerElement(id = "start", label = "Start Date", required = true),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Start Date *").assertExists()
    }

    @Test
    fun displays_current_value() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicDatePicker(
                    element = DatePickerElement(id = "start", label = "Start Date"),
                    value = "2026-01-15",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("2026-01-15").assertExists()
    }

    @Test
    fun displays_error_message() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicDatePicker(
                    element = DatePickerElement(id = "start", label = "Start Date", required = true),
                    value = "",
                    error = "Required",
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Required").assertExists()
    }
}
