package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lfr.dynamicforms.domain.model.MultiSelectElement
import com.lfr.dynamicforms.domain.model.SelectOption
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DynamicMultiSelectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_label() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicMultiSelect(
                    element = MultiSelectElement(
                        id = "skills",
                        label = "Skills",
                        options = listOf(
                            SelectOption("kt", "Kotlin"),
                            SelectOption("java", "Java"),
                            SelectOption("py", "Python")
                        )
                    ),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Skills").assertExists()
    }

    @Test
    fun displays_all_options() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicMultiSelect(
                    element = MultiSelectElement(
                        id = "skills",
                        label = "Skills",
                        options = listOf(
                            SelectOption("kt", "Kotlin"),
                            SelectOption("java", "Java"),
                            SelectOption("py", "Python")
                        )
                    ),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Kotlin").assertExists()
        composeTestRule.onNodeWithText("Java").assertExists()
        composeTestRule.onNodeWithText("Python").assertExists()
    }

    @Test
    fun toggling_checkbox_invokes_callback() {
        var invoked = false

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicMultiSelect(
                    element = MultiSelectElement(
                        id = "skills",
                        label = "Skills",
                        options = listOf(
                            SelectOption("kt", "Kotlin"),
                            SelectOption("java", "Java"),
                            SelectOption("py", "Python")
                        )
                    ),
                    value = "",
                    error = null,
                    onValueChange = { invoked = true }
                )
            }
        }

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        assertTrue(invoked)
    }

    @Test
    fun displays_error_message() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicMultiSelect(
                    element = MultiSelectElement(
                        id = "skills",
                        label = "Skills",
                        options = listOf(
                            SelectOption("kt", "Kotlin"),
                            SelectOption("java", "Java"),
                            SelectOption("py", "Python")
                        )
                    ),
                    value = "",
                    error = "Select at least one",
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Select at least one").assertExists()
    }
}
