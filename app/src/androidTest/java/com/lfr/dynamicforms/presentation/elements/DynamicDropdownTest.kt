package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lfr.dynamicforms.domain.model.DropdownElement
import com.lfr.dynamicforms.domain.model.SelectOption
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import org.junit.Rule
import org.junit.Test

class DynamicDropdownTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_label() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicDropdown(
                    element = DropdownElement(
                        id = "dept",
                        label = "Department",
                        required = true,
                        options = listOf(
                            SelectOption("eng", "Engineering"),
                            SelectOption("sales", "Sales")
                        )
                    ),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Department *").assertExists()
    }

    @Test
    fun displays_selected_option_label() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicDropdown(
                    element = DropdownElement(
                        id = "dept",
                        label = "Department",
                        options = listOf(
                            SelectOption("eng", "Engineering"),
                            SelectOption("sales", "Sales")
                        )
                    ),
                    value = "eng",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Engineering").assertExists()
    }

    @Test
    fun displays_error_message() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicDropdown(
                    element = DropdownElement(
                        id = "dept",
                        label = "Department",
                        required = true,
                        options = listOf(
                            SelectOption("eng", "Engineering"),
                            SelectOption("sales", "Sales")
                        )
                    ),
                    value = "",
                    error = "Required",
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Required").assertExists()
    }
}
