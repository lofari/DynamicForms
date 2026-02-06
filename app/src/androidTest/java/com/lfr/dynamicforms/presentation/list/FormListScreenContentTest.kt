package com.lfr.dynamicforms.presentation.list

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import org.junit.Rule
import org.junit.Test

class FormListScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loading_state_shows_progress_indicator() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormListScreenContent(
                    state = FormListState(isLoading = true),
                    onFormClick = {}
                )
            }
        }

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertExists()
    }

    @Test
    fun error_state_shows_error_text() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormListScreenContent(
                    state = FormListState(isLoading = false, errorMessage = "Network error"),
                    onFormClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Network error").assertExists()
    }

    @Test
    fun forms_are_rendered_with_titles() {
        val forms = listOf(
            FormSummary("f1", "Safety Inspection", "Monthly checklist", 3, 12),
            FormSummary("f2", "Employee Onboarding", "New hire form", 5, 24)
        )

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormListScreenContent(
                    state = FormListState(isLoading = false, forms = forms),
                    onFormClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Safety Inspection").assertExists()
        composeTestRule.onNodeWithText("Employee Onboarding").assertExists()
    }

    @Test
    fun metadata_badges_show_counts() {
        val forms = listOf(
            FormSummary("f1", "Safety Inspection", "Monthly checklist", 3, 12)
        )

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormListScreenContent(
                    state = FormListState(isLoading = false, forms = forms),
                    onFormClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("3 steps").assertExists()
        composeTestRule.onNodeWithText("12 fields").assertExists()
    }

    @Test
    fun resume_badge_shown_for_drafts() {
        val forms = listOf(
            FormSummary("f1", "Safety Inspection", "Monthly checklist", 3, 12)
        )

        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                FormListScreenContent(
                    state = FormListState(isLoading = false, forms = forms, drafts = setOf("f1")),
                    onFormClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Resume").assertExists()
    }
}
