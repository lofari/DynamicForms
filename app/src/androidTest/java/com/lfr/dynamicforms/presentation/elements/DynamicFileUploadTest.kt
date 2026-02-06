package com.lfr.dynamicforms.presentation.elements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lfr.dynamicforms.domain.model.FileUploadElement
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme
import org.junit.Rule
import org.junit.Test

class DynamicFileUploadTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_label() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicFileUpload(
                    element = FileUploadElement(
                        id = "doc",
                        label = "Upload Document",
                        required = true,
                        allowedTypes = listOf("pdf", "docx")
                    ),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Upload Document *").assertExists()
    }

    @Test
    fun shows_choose_file_when_empty() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicFileUpload(
                    element = FileUploadElement(id = "doc", label = "Upload Document"),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Choose file").assertExists()
    }

    @Test
    fun shows_filename_when_selected() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicFileUpload(
                    element = FileUploadElement(id = "doc", label = "Upload Document"),
                    value = "report.pdf",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("report.pdf").assertExists()
    }

    @Test
    fun shows_allowed_types() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicFileUpload(
                    element = FileUploadElement(
                        id = "doc",
                        label = "Upload Document",
                        allowedTypes = listOf("pdf", "docx")
                    ),
                    value = "",
                    error = null,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Allowed: pdf, docx").assertExists()
    }

    @Test
    fun displays_error_message() {
        composeTestRule.setContent {
            DynamicFormsTheme(dynamicColor = false) {
                DynamicFileUpload(
                    element = FileUploadElement(id = "doc", label = "Upload Document", required = true),
                    value = "",
                    error = "File required",
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("File required").assertExists()
    }
}
