package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lfr.dynamicforms.domain.model.RepeatingGroupElement
import com.lfr.dynamicforms.domain.model.TextFieldElement
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DynamicRepeatingGroupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_group_label() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicRepeatingGroup(
                    element = RepeatingGroupElement(
                        id = "contacts",
                        label = "Emergency Contacts",
                        minItems = 1,
                        maxItems = 3,
                        elements = listOf(TextFieldElement(id = "name", label = "Name"))
                    ),
                    values = emptyMap(),
                    errors = emptyMap(),
                    itemCount = 1,
                    onValueChange = { _, _ -> },
                    onAddRow = {},
                    onRemoveRow = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Emergency Contacts").assertExists()
    }

    @Test
    fun displays_item_cards() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicRepeatingGroup(
                    element = RepeatingGroupElement(
                        id = "contacts",
                        label = "Emergency Contacts",
                        minItems = 1,
                        maxItems = 5,
                        elements = listOf(TextFieldElement(id = "name", label = "Name"))
                    ),
                    values = emptyMap(),
                    errors = emptyMap(),
                    itemCount = 2,
                    onValueChange = { _, _ -> },
                    onAddRow = {},
                    onRemoveRow = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Item 1").assertExists()
        composeTestRule.onNodeWithText("Item 2").assertExists()
    }

    @Test
    fun shows_add_button() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicRepeatingGroup(
                    element = RepeatingGroupElement(
                        id = "contacts",
                        label = "Emergency Contacts",
                        minItems = 1,
                        maxItems = 3,
                        elements = listOf(TextFieldElement(id = "name", label = "Name"))
                    ),
                    values = emptyMap(),
                    errors = emptyMap(),
                    itemCount = 1,
                    onValueChange = { _, _ -> },
                    onAddRow = {},
                    onRemoveRow = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Add Emergency Contacts").assertExists()
    }

    @Test
    fun hides_add_button_at_max() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicRepeatingGroup(
                    element = RepeatingGroupElement(
                        id = "contacts",
                        label = "Emergency Contacts",
                        minItems = 1,
                        maxItems = 3,
                        elements = listOf(TextFieldElement(id = "name", label = "Name"))
                    ),
                    values = emptyMap(),
                    errors = emptyMap(),
                    itemCount = 3,
                    onValueChange = { _, _ -> },
                    onAddRow = {},
                    onRemoveRow = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Add Emergency Contacts").assertDoesNotExist()
    }

    @Test
    fun add_button_invokes_callback() {
        var invoked = false

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicRepeatingGroup(
                    element = RepeatingGroupElement(
                        id = "contacts",
                        label = "Emergency Contacts",
                        minItems = 1,
                        maxItems = 3,
                        elements = listOf(TextFieldElement(id = "name", label = "Name"))
                    ),
                    values = emptyMap(),
                    errors = emptyMap(),
                    itemCount = 1,
                    onValueChange = { _, _ -> },
                    onAddRow = { invoked = true },
                    onRemoveRow = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Add Emergency Contacts").performClick()
        assertTrue(invoked)
    }
}
