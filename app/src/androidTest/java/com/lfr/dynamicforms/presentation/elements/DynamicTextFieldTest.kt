package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.lfr.dynamicforms.domain.model.TextFieldElement
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DynamicTextFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_label_with_required_asterisk() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicTextField(
                    element = TextFieldElement(id = "name", label = "Full Name", required = true),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Full Name *").assertExists()
    }

    @Test
    fun displays_current_value() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicTextField(
                    element = TextFieldElement(id = "name", label = "Full Name"),
                    value = "Jane",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Jane").assertExists()
    }

    @Test
    fun displays_error_message() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicTextField(
                    element = TextFieldElement(id = "name", label = "Full Name", required = true),
                    value = "",
                    error = "Required",
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Required").assertExists()
    }

    @Test
    fun typing_invokes_onValueChange() {
        var invoked = false

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicTextField(
                    element = TextFieldElement(id = "name", label = "Full Name", required = true),
                    value = "",
                    error = null,
                    onValueChange = { invoked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Full Name *").performTextInput("test")
        assertTrue(invoked)
    }
}
