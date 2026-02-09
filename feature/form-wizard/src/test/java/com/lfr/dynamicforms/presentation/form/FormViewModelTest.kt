package com.lfr.dynamicforms.presentation.form

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.lfr.dynamicforms.testing.MainDispatcherRule
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.Page
import com.lfr.dynamicforms.domain.model.RepeatingGroupElement
import com.lfr.dynamicforms.domain.model.TextFieldElement
import com.lfr.dynamicforms.domain.usecase.EvaluateVisibilityUseCase
import com.lfr.dynamicforms.domain.usecase.EnqueueSubmissionUseCase
import com.lfr.dynamicforms.domain.usecase.FormWithDraft
import com.lfr.dynamicforms.domain.usecase.GetFormUseCase
import com.lfr.dynamicforms.domain.usecase.SaveDraftUseCase
import com.lfr.dynamicforms.domain.usecase.SubmitFormUseCase
import com.lfr.dynamicforms.domain.usecase.ValidatePageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
class FormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getForm = mockk<GetFormUseCase>()
    private val saveDraft = mockk<SaveDraftUseCase>(relaxUnitFun = true)
    private val submitForm = mockk<SubmitFormUseCase>()
    private val enqueueSubmission = mockk<EnqueueSubmissionUseCase>()
    private val validatePage = mockk<ValidatePageUseCase>()
    private val evaluateVisibility = mockk<EvaluateVisibilityUseCase>(relaxed = true)

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        FormViewModel(savedStateHandle, getForm, saveDraft, submitForm, enqueueSubmission, validatePage, evaluateVisibility)

    private fun loadedViewModel(pages: Int = 2): FormViewModel {
        val pageList = (1..pages).map {
            Page("p$it", "Page $it", listOf(TextFieldElement("f$it", "Field $it")))
        }
        val form = Form("f1", "Test", pageList)
        coEvery { getForm("f1") } returns DomainResult.Success(FormWithDraft(form, emptyMap(), 0))
        val vm = createViewModel()
        vm.onAction(FormAction.LoadForm("f1"))
        return vm
    }

    // --- LoadForm tests ---

    @Test
    fun `LoadForm populates form and values`() = runTest {
        val page = Page("p1", "Page 1", listOf(TextFieldElement("name", "Name")))
        val form = Form("f1", "Test Form", listOf(page))
        val initialValues = mapOf("name" to "Alice")
        coEvery { getForm("f1") } returns DomainResult.Success(FormWithDraft(form, initialValues, 0))

        val vm = createViewModel()
        vm.onAction(FormAction.LoadForm("f1"))

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(form, state.form)
        assertEquals("Alice", state.values["name"])
        assertEquals(0, state.currentPageIndex)
    }

    @Test
    fun `LoadForm initializes repeatingGroupCounts`() = runTest {
        val rg = RepeatingGroupElement(
            id = "rg1", label = "Group", minItems = 2, elements = listOf(
                TextFieldElement("rg1_field", "Field")
            )
        )
        val page = Page("p1", "Page 1", listOf(rg))
        val form = Form("f1", "Test Form", listOf(page))
        coEvery { getForm("f1") } returns DomainResult.Success(FormWithDraft(form, emptyMap(), 0))

        val vm = createViewModel()
        vm.onAction(FormAction.LoadForm("f1"))

        assertEquals(2, vm.state.value.repeatingGroupCounts["rg1"])
    }

    @Test
    fun `LoadForm error sets errorMessage`() = runTest {
        coEvery { getForm("f1") } returns DomainResult.Failure(RuntimeException("Oops"))

        val vm = createViewModel()
        vm.onAction(FormAction.LoadForm("f1"))

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals("Something went wrong. Please try again.", state.errorMessage)
    }

    // --- UpdateField tests ---

    @Test
    fun `UpdateField adds value to state`() = runTest {
        val vm = loadedViewModel()
        vm.onAction(FormAction.UpdateField("name", "Jane"))

        assertEquals("Jane", vm.state.value.values["name"])
    }

    @Test
    fun `UpdateField clears field error`() = runTest {
        val page = Page("p1", "Page 1", listOf(TextFieldElement("name", "Name")))
        val form = Form("f1", "Test", listOf(page))
        coEvery { getForm("f1") } returns DomainResult.Success(FormWithDraft(form, emptyMap(), 0))

        val vm = createViewModel()
        vm.onAction(FormAction.LoadForm("f1"))

        // Simulate errors by triggering validation failure on NextPage
        every { validatePage.validate(any(), any()) } returns mapOf("name" to "Required")
        vm.onAction(FormAction.NextPage)
        assertTrue(vm.state.value.errors.containsKey("name"))

        // UpdateField should clear the error for that field
        vm.onAction(FormAction.UpdateField("name", "Jane"))
        assertFalse(vm.state.value.errors.containsKey("name"))
    }

    // --- Navigation tests ---

    @Test
    fun `NextPage advances when valid`() = runTest {
        val vm = loadedViewModel(pages = 3)
        every { validatePage.validate(any(), any()) } returns emptyMap()

        vm.onAction(FormAction.NextPage)

        assertEquals(1, vm.state.value.currentPageIndex)
    }

    @Test
    fun `NextPage sets errors without advancing when invalid`() = runTest {
        val vm = loadedViewModel(pages = 3)
        every { validatePage.validate(any(), any()) } returns mapOf("f1" to "Required")

        vm.onAction(FormAction.NextPage)

        assertEquals(0, vm.state.value.currentPageIndex)
        assertEquals("Required", vm.state.value.errors["f1"])
    }

    @Test
    fun `NextPage saves draft`() = runTest {
        val vm = loadedViewModel(pages = 3)
        every { validatePage.validate(any(), any()) } returns emptyMap()

        vm.onAction(FormAction.NextPage)

        coVerify { saveDraft(eq("f1"), any(), any()) }
    }

    @Test
    fun `PrevPage decrements and clamps to 0`() = runTest {
        val vm = loadedViewModel(pages = 3)
        every { validatePage.validate(any(), any()) } returns emptyMap()

        // Move to page 1
        vm.onAction(FormAction.NextPage)
        assertEquals(1, vm.state.value.currentPageIndex)

        // Move back to page 0
        vm.onAction(FormAction.PrevPage)
        assertEquals(0, vm.state.value.currentPageIndex)

        // PrevPage again should clamp to 0
        vm.onAction(FormAction.PrevPage)
        assertEquals(0, vm.state.value.currentPageIndex)
    }

    // --- Submit tests ---

    @Test
    fun `Submit enqueues and emits NavigateToSuccess`() = runTest {
        val vm = loadedViewModel()
        every { validatePage.validateAllPages(any(), any()) } returns emptyMap()
        coEvery { enqueueSubmission(any(), any(), any()) } returns "uuid-123"

        vm.effect.test {
            vm.onAction(FormAction.Submit)
            val effect = awaitItem()
            assertTrue(effect is FormEffect.NavigateToSuccess)
            assertEquals("Form queued for submission", (effect as FormEffect.NavigateToSuccess).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Submit calls EnqueueSubmissionUseCase`() = runTest {
        val vm = loadedViewModel()
        every { validatePage.validateAllPages(any(), any()) } returns emptyMap()
        coEvery { enqueueSubmission(any(), any(), any()) } returns "uuid-123"

        vm.effect.test {
            vm.onAction(FormAction.Submit)
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { enqueueSubmission("f1", "Test", any()) }
    }

    @Test
    fun `Submit sets isSubmitting false after completion`() = runTest {
        val vm = loadedViewModel()
        every { validatePage.validateAllPages(any(), any()) } returns emptyMap()
        coEvery { enqueueSubmission(any(), any(), any()) } returns "uuid-123"

        vm.effect.test {
            vm.onAction(FormAction.Submit)
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `Submit validation failure sets errors and page without enqueuing`() = runTest {
        val vm = loadedViewModel(pages = 3)
        val errors = mapOf("f2" to "Required")
        every { validatePage.validateAllPages(any(), any()) } returns errors
        every { validatePage.firstPageWithErrors(any(), errors) } returns 1

        vm.onAction(FormAction.Submit)

        val state = vm.state.value
        assertEquals(errors, state.errors)
        assertEquals(1, state.currentPageIndex)
        coVerify(exactly = 0) { enqueueSubmission(any(), any(), any()) }
    }

    @Test
    fun `Submit enqueue error emits ShowError`() = runTest {
        val vm = loadedViewModel()
        every { validatePage.validateAllPages(any(), any()) } returns emptyMap()
        coEvery { enqueueSubmission(any(), any(), any()) } throws RuntimeException("DB error")

        vm.effect.test {
            vm.onAction(FormAction.Submit)
            val effect = awaitItem()
            assertTrue(effect is FormEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Repeating group tests ---

    @Test
    fun `AddRepeatingRow increments count`() = runTest {
        val rg = RepeatingGroupElement(
            id = "rg1", label = "Group", minItems = 2, elements = listOf(
                TextFieldElement("rg1_field", "Field")
            )
        )
        val page = Page("p1", "Page 1", listOf(rg))
        val form = Form("f1", "Test", listOf(page))
        coEvery { getForm("f1") } returns DomainResult.Success(FormWithDraft(form, emptyMap(), 0))

        val vm = createViewModel()
        vm.onAction(FormAction.LoadForm("f1"))
        assertEquals(2, vm.state.value.repeatingGroupCounts["rg1"])

        vm.onAction(FormAction.AddRepeatingRow("rg1"))
        assertEquals(3, vm.state.value.repeatingGroupCounts["rg1"])
    }

    @Test
    fun `RemoveRepeatingRow shifts values and decrements`() = runTest {
        val rg = RepeatingGroupElement(
            id = "rg1", label = "Group", minItems = 1, elements = listOf(
                TextFieldElement("name", "Name")
            )
        )
        val page = Page("p1", "Page 1", listOf(rg))
        val form = Form("f1", "Test", listOf(page))
        val initialValues = mapOf(
            "rg1[0].name" to "A",
            "rg1[1].name" to "B",
            "rg1[2].name" to "C"
        )
        coEvery { getForm("f1") } returns DomainResult.Success(FormWithDraft(form, initialValues, 0))

        val vm = createViewModel()
        vm.onAction(FormAction.LoadForm("f1"))

        // Manually set repeatingGroupCounts to 3 (since minItems=1, loadForm sets it to 1)
        // We need to add rows to get to count 3
        vm.onAction(FormAction.AddRepeatingRow("rg1"))
        vm.onAction(FormAction.AddRepeatingRow("rg1"))
        assertEquals(3, vm.state.value.repeatingGroupCounts["rg1"])

        // Remove row at index 1 (value "B")
        vm.onAction(FormAction.RemoveRepeatingRow("rg1", 1))

        val state = vm.state.value
        assertEquals(2, state.repeatingGroupCounts["rg1"])
        assertEquals("A", state.values["rg1[0].name"])
        assertEquals("C", state.values["rg1[1].name"])
        assertNull(state.values["rg1[2].name"])
    }
}
