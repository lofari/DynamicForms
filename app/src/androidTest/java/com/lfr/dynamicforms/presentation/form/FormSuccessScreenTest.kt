package com.lfr.dynamicforms.presentation.form

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FormSuccessScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_success_heading() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormSuccessScreen(
                    message = "Done",
                    onBackToList = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Success!").assertExists()
    }

    @Test
    fun displays_message() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormSuccessScreen(
                    message = "Your form was submitted",
                    onBackToList = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Your form was submitted").assertExists()
    }

    @Test
    fun back_button_click_invokes_callback() {
        var clicked = false

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormSuccessScreen(
                    message = "Done",
                    onBackToList = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Back to Forms").performClick()
        assertTrue(clicked)
    }
}
