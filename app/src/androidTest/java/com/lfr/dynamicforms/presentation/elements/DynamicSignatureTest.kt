package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lfr.dynamicforms.domain.model.SignatureElement
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme
import org.junit.Rule
import org.junit.Test

class DynamicSignatureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_label() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicSignature(
                    element = SignatureElement(id = "sig", label = "Signature", required = true),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Signature *").assertExists()
    }

    @Test
    fun shows_placeholder_when_empty() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicSignature(
                    element = SignatureElement(id = "sig", label = "Signature"),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Draw your signature here").assertExists()
    }

    @Test
    fun shows_clear_button_when_signed() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicSignature(
                    element = SignatureElement(id = "sig", label = "Signature"),
                    value = "signed",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Clear").assertExists()
    }

    @Test
    fun hides_placeholder_when_signed() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicSignature(
                    element = SignatureElement(id = "sig", label = "Signature"),
                    value = "signed",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Draw your signature here").assertDoesNotExist()
    }

    @Test
    fun displays_error_message() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicSignature(
                    element = SignatureElement(id = "sig", label = "Signature", required = true),
                    value = "",
                    error = "Signature required",
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Signature required").assertExists()
    }
}
