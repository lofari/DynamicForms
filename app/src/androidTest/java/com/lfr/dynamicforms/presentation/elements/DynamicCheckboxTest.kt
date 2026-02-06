package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lfr.dynamicforms.domain.model.CheckboxElement
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DynamicCheckboxTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_label() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicCheckbox(
                    element = CheckboxElement(id = "agree", label = "I agree"),
                    value = "false",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("I agree").assertExists()
    }

    @Test
    fun checked_state_from_value() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicCheckbox(
                    element = CheckboxElement(id = "agree", label = "I agree"),
                    value = "true",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun click_invokes_onValueChange() {
        var invoked = false

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicCheckbox(
                    element = CheckboxElement(id = "agree", label = "I agree"),
                    value = "false",
                    error = null,
                    onValueChange = { invoked = true }
                )
            }
        }

        composeTestRule.onNode(isToggleable()).performClick()
        assertTrue(invoked)
    }
}
