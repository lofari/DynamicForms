package com.lfr.dynamicforms.presentation.list

import com.lfr.dynamicforms.MainDispatcherRule
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val formRepo = mockk<FormRepository>()
    private val draftRepo = mockk<DraftRepository>()

    private fun createViewModel() = FormListViewModel(formRepo, draftRepo)

    @Test
    fun `initial load populates forms and drafts`() = runTest {
        val forms = listOf(
            FormSummary("f1", "Form 1", "Desc 1", pageCount = 2, fieldCount = 5),
            FormSummary("f2", "Form 2", "Desc 2", pageCount = 3, fieldCount = 8)
        )
        coEvery { formRepo.getForms() } returns forms
        coEvery { draftRepo.getFormIdsWithDrafts() } returns listOf("f1")

        val vm = createViewModel()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(forms, state.forms)
        assertEquals(setOf("f1"), state.drafts)
        assertNull(state.errorMessage)
    }

    @Test
    fun `initial load error sets errorMessage`() = runTest {
        coEvery { formRepo.getForms() } throws RuntimeException("Network error")

        val vm = createViewModel()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.errorMessage)
        assertTrue(state.forms.isEmpty())
    }

    @Test
    fun `refresh reloads forms`() = runTest {
        val initialForms = listOf(FormSummary("f1", "Form 1"))
        coEvery { formRepo.getForms() } returns initialForms
        coEvery { draftRepo.getFormIdsWithDrafts() } returns emptyList()

        val vm = createViewModel()
        assertEquals(initialForms, vm.state.value.forms)

        // Change mock return for refresh
        val updatedForms = listOf(
            FormSummary("f1", "Form 1"),
            FormSummary("f2", "Form 2")
        )
        coEvery { formRepo.getForms() } returns updatedForms

        vm.refresh()

        assertEquals(updatedForms, vm.state.value.forms)
    }

    @Test
    fun `empty drafts returns empty set`() = runTest {
        coEvery { formRepo.getForms() } returns listOf(FormSummary("f1", "Form 1"))
        coEvery { draftRepo.getFormIdsWithDrafts() } returns emptyList()

        val vm = createViewModel()

        assertEquals(emptySet<String>(), vm.state.value.drafts)
    }

    @Test
    fun `forms with matching drafts are in drafts set`() = runTest {
        val forms = listOf(
            FormSummary("f1", "Form 1"),
            FormSummary("f2", "Form 2"),
            FormSummary("f3", "Form 3")
        )
        coEvery { formRepo.getForms() } returns forms
        coEvery { draftRepo.getFormIdsWithDrafts() } returns listOf("f1", "f3")

        val vm = createViewModel()

        val drafts = vm.state.value.drafts
        assertEquals(setOf("f1", "f3"), drafts)
        assertFalse(drafts.contains("f2"))
    }
}
