package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.lfr.dynamicforms.domain.model.NumberFieldElement
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DynamicNumberFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_label_with_required_asterisk() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicNumberField(
                    element = NumberFieldElement(id = "qty", label = "Quantity", required = true),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Quantity *").assertExists()
    }

    @Test
    fun displays_current_value() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicNumberField(
                    element = NumberFieldElement(id = "qty", label = "Quantity"),
                    value = "42",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("42").assertExists()
    }

    @Test
    fun displays_error_message() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicNumberField(
                    element = NumberFieldElement(id = "qty", label = "Quantity", required = true),
                    value = "999",
                    error = "Too large",
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Too large").assertExists()
    }

    @Test
    fun typing_invokes_onValueChange() {
        var invoked = false

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicNumberField(
                    element = NumberFieldElement(id = "qty", label = "Quantity", required = true),
                    value = "",
                    error = null,
                    onValueChange = { invoked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Quantity *").performTextInput("5")
        assertTrue(invoked)
    }
}
