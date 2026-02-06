package com.lfr.dynamicforms.presentation.form

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lfr.dynamicforms.domain.model.*
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import org.junit.Rule
import org.junit.Test

class FormScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun loadedState(pageCount: Int = 2, currentPageIndex: Int = 0): FormUiState {
        val pages = (1..pageCount).map { i ->
            Page("p$i", "Page $i", listOf(
                SectionHeaderElement(id = "sh$i", label = "Section $i"),
                TextFieldElement(id = "tf$i", label = "Field $i")
            ))
        }
        return FormUiState(
            isLoading = false,
            form = Form("f1", "Test Form", pages),
            currentPageIndex = currentPageIndex
        )
    }

    @Test
    fun loading_state_shows_progress_indicator() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormScreenContent(
                    state = FormUiState(isLoading = true),
                    onAction = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertExists()
    }

    @Test
    fun error_state_shows_error_message() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormScreenContent(
                    state = FormUiState(isLoading = false, errorMessage = "Oops"),
                    onAction = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Oops").assertExists()
    }

    @Test
    fun loaded_form_shows_page_title_in_app_bar() {
        val state = loadedState(pageCount = 2, currentPageIndex = 0)

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormScreenContent(
                    state = state,
                    onAction = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Page 1").assertExists()
    }

    @Test
    fun progress_shows_step_counter() {
        val state = loadedState(pageCount = 2, currentPageIndex = 0)

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormScreenContent(
                    state = state,
                    onAction = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Step 1 of 2").assertExists()
    }

    @Test
    fun next_button_visible_on_non_last_page() {
        val state = loadedState(pageCount = 2, currentPageIndex = 0)

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormScreenContent(
                    state = state,
                    onAction = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Next").assertExists()
        composeTestRule.onNodeWithText("Submit").assertDoesNotExist()
    }

    @Test
    fun submit_button_visible_on_last_page() {
        val state = loadedState(pageCount = 2, currentPageIndex = 1)

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormScreenContent(
                    state = state,
                    onAction = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Submit").assertExists()
        composeTestRule.onNodeWithText("Next").assertDoesNotExist()
    }

    @Test
    fun back_button_visible_on_non_first_page() {
        val state = loadedState(pageCount = 2, currentPageIndex = 1)

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormScreenContent(
                    state = state,
                    onAction = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Back").assertExists()
    }

    @Test
    fun section_header_rendered() {
        val page = Page("p1", "Page 1", listOf(
            SectionHeaderElement(id = "sh1", label = "Contact Details")
        ))
        val state = FormUiState(
            isLoading = false,
            form = Form("f1", "Test Form", listOf(page)),
            currentPageIndex = 0
        )

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormScreenContent(
                    state = state,
                    onAction = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Contact Details").assertExists()
    }
}
