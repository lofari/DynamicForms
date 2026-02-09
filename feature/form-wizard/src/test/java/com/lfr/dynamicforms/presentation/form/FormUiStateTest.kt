package com.lfr.dynamicforms.presentation.form

import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.Page
import com.lfr.dynamicforms.domain.model.TextFieldElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormUiStateTest {

    private fun makePage(id: String) = Page(
        pageId = id,
        title = "Page $id",
        elements = listOf(TextFieldElement(id = "field_$id", label = "Field $id"))
    )

    private fun makeForm(pageCount: Int) = Form(
        formId = "f1",
        title = "Test Form",
        pages = (1..pageCount).map { makePage("p$it") }
    )

    @Test
    fun `totalPages returns 0 when form is null`() {
        val state = FormUiState()
        assertEquals(0, state.totalPages)
    }

    @Test
    fun `totalPages returns page count`() {
        val state = FormUiState(form = makeForm(3))
        assertEquals(3, state.totalPages)
    }

    @Test
    fun `isFirstPage true at index 0`() {
        val state = FormUiState(form = makeForm(3), currentPageIndex = 0)
        assertTrue(state.isFirstPage)
    }

    @Test
    fun `isFirstPage false at index 1`() {
        val state = FormUiState(form = makeForm(3), currentPageIndex = 1)
        assertFalse(state.isFirstPage)
    }

    @Test
    fun `isLastPage true at last index`() {
        val state = FormUiState(form = makeForm(3), currentPageIndex = 2)
        assertTrue(state.isLastPage)
    }

    @Test
    fun `currentPage returns correct page`() {
        val form = makeForm(3)
        val state = FormUiState(form = form, currentPageIndex = 1)
        assertEquals(form.pages[1], state.currentPage)
    }

    @Test
    fun `progressFraction calculation`() {
        val state = FormUiState(form = makeForm(3), currentPageIndex = 1)
        assertEquals(2f / 3f, state.progressFraction, 0.0001f)

        val emptyState = FormUiState()
        assertEquals(0f, emptyState.progressFraction, 0.0001f)
    }
}
