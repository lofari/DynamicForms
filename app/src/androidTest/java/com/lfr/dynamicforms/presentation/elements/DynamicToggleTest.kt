package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lfr.dynamicforms.domain.model.ToggleElement
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DynamicToggleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_label() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicToggle(
                    element = ToggleElement(id = "notify", label = "Enable notifications"),
                    value = "false",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Enable notifications").assertExists()
    }

    @Test
    fun switch_on_when_value_true() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicToggle(
                    element = ToggleElement(id = "notify", label = "Enable notifications"),
                    value = "true",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun switch_off_when_value_false() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicToggle(
                    element = ToggleElement(id = "notify", label = "Enable notifications"),
                    value = "false",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun toggling_invokes_callback() {
        var invoked = false

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicToggle(
                    element = ToggleElement(id = "notify", label = "Enable notifications"),
                    value = "false",
                    error = null,
                    onValueChange = { invoked = true }
                )
            }
        }

        composeTestRule.onNode(isToggleable()).performClick()
        assertTrue(invoked)
    }

    @Test
    fun displays_error_message() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicToggle(
                    element = ToggleElement(id = "notify", label = "Enable notifications", required = true),
                    value = "false",
                    error = "Required",
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Required").assertExists()
    }
}
