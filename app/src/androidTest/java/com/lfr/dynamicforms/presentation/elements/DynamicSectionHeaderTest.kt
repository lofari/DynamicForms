package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lfr.dynamicforms.domain.model.SectionHeaderElement
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import org.junit.Rule
import org.junit.Test

class DynamicSectionHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_label() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicSectionHeader(
                    element = SectionHeaderElement(id = "sh1", label = "Personal Info")
                )
            }
        }

        composeTestRule.onNodeWithText("Personal Info").assertExists()
    }

    @Test
    fun displays_subtitle() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicSectionHeader(
                    element = SectionHeaderElement(
                        id = "sh1",
                        label = "Personal Info",
                        subtitle = "Enter your details"
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Enter your details").assertExists()
    }

    @Test
    fun hides_subtitle_when_null() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicSectionHeader(
                    element = SectionHeaderElement(
                        id = "sh1",
                        label = "Personal Info",
                        subtitle = null
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Personal Info").assertExists()
        composeTestRule.onNodeWithText("Enter your details").assertDoesNotExist()
    }
}
