package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lfr.dynamicforms.domain.model.LabelElement
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme
import org.junit.Rule
import org.junit.Test

class DynamicLabelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_text() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicLabel(
                    element = LabelElement(id = "lbl1", label = "Label", text = "Please review")
                )
            }
        }

        composeTestRule.onNodeWithText("Please review").assertExists()
    }

    @Test
    fun falls_back_to_label_when_text_blank() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicLabel(
                    element = LabelElement(id = "lbl1", label = "Fallback Label", text = "")
                )
            }
        }

        composeTestRule.onNodeWithText("Fallback Label").assertExists()
    }
}
