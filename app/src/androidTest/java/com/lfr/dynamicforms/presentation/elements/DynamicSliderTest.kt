package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.lfr.dynamicforms.domain.model.SliderElement
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme
import org.junit.Rule
import org.junit.Test

class DynamicSliderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_label_with_value() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicSlider(
                    element = SliderElement(id = "rating", label = "Rating", min = 0f, max = 10f, step = 1f),
                    value = "7",
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Rating: 7").assertExists()
    }

    @Test
    fun slider_exists_with_test_tag() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicSlider(
                    element = SliderElement(id = "rating", label = "Rating", min = 0f, max = 10f, step = 1f),
                    value = "7",
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("field_rating").assertExists()
    }
}
