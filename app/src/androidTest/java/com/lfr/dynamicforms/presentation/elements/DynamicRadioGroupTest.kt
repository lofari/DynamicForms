package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lfr.dynamicforms.domain.model.RadioElement
import com.lfr.dynamicforms.domain.model.SelectOption
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DynamicRadioGroupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_label() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicRadioGroup(
                    element = RadioElement(
                        id = "priority",
                        label = "Priority",
                        options = listOf(
                            SelectOption("low", "Low"),
                            SelectOption("med", "Medium"),
                            SelectOption("high", "High")
                        )
                    ),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Priority").assertExists()
    }

    @Test
    fun displays_all_options() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicRadioGroup(
                    element = RadioElement(
                        id = "priority",
                        label = "Priority",
                        options = listOf(
                            SelectOption("low", "Low"),
                            SelectOption("med", "Medium"),
                            SelectOption("high", "High")
                        )
                    ),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Low").assertExists()
        composeTestRule.onNodeWithText("Medium").assertExists()
        composeTestRule.onNodeWithText("High").assertExists()
    }

    @Test
    fun selecting_option_invokes_callback() {
        var invoked = false

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicRadioGroup(
                    element = RadioElement(
                        id = "priority",
                        label = "Priority",
                        options = listOf(
                            SelectOption("low", "Low"),
                            SelectOption("med", "Medium"),
                            SelectOption("high", "High")
                        )
                    ),
                    value = "",
                    error = null,
                    onValueChange = { invoked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("High").performClick()
        assertTrue(invoked)
    }

    @Test
    fun displays_error_message() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicRadioGroup(
                    element = RadioElement(
                        id = "priority",
                        label = "Priority",
                        options = listOf(
                            SelectOption("low", "Low"),
                            SelectOption("med", "Medium"),
                            SelectOption("high", "High")
                        )
                    ),
                    value = "",
                    error = "Please select",
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Please select").assertExists()
    }
}
