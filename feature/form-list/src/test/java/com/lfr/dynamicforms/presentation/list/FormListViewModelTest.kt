package com.lfr.dynamicforms.presentation.list

import com.lfr.dynamicforms.testing.MainDispatcherRule
import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.SubmissionStatus
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import com.lfr.dynamicforms.domain.usecase.SyncWorkScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val submissionQueueRepo = mockk<SubmissionQueueRepository>(relaxUnitFun = true)
    private val syncWorkScheduler = mockk<SyncWorkScheduler>(relaxUnitFun = true)
    private val pendingFlow = MutableStateFlow<List<PendingSubmission>>(emptyList())

    init {
        coEvery { submissionQueueRepo.observeAll() } returns pendingFlow
        coEvery { submissionQueueRepo.retry(any()) } returns DomainResult.Success(Unit)
        coEvery { submissionQueueRepo.delete(any()) } returns DomainResult.Success(Unit)
    }

    private fun createViewModel() = FormListViewModel(formRepo, draftRepo, submissionQueueRepo, syncWorkScheduler)

    @Test
    fun `initial load populates forms and drafts`() = runTest {
        val forms = listOf(
            FormSummary("f1", "Form 1", "Desc 1", pageCount = 2, fieldCount = 5),
            FormSummary("f2", "Form 2", "Desc 2", pageCount = 3, fieldCount = 8)
        )
        coEvery { formRepo.getForms() } returns DomainResult.Success(forms)
        coEvery { draftRepo.getFormIdsWithDrafts() } returns DomainResult.Success(listOf("f1"))

        val vm = createViewModel()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(forms, state.forms)
        assertEquals(setOf("f1"), state.drafts)
        assertNull(state.errorMessage)
    }

    @Test
    fun `initial load error sets errorMessage`() = runTest {
        coEvery { formRepo.getForms() } returns DomainResult.Failure(DomainError.Unknown())

        val vm = createViewModel()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals("Something went wrong. Please try again.", state.errorMessage)
        assertTrue(state.forms.isEmpty())
    }

    @Test
    fun `refresh reloads forms`() = runTest {
        val initialForms = listOf(FormSummary("f1", "Form 1"))
        coEvery { formRepo.getForms() } returns DomainResult.Success(initialForms)
        coEvery { draftRepo.getFormIdsWithDrafts() } returns DomainResult.Success(emptyList())

        val vm = createViewModel()
        assertEquals(initialForms, vm.state.value.forms)

        // Change mock return for refresh
        val updatedForms = listOf(
            FormSummary("f1", "Form 1"),
            FormSummary("f2", "Form 2")
        )
        coEvery { formRepo.getForms() } returns DomainResult.Success(updatedForms)

        vm.refresh()

        assertEquals(updatedForms, vm.state.value.forms)
    }

    @Test
    fun `empty drafts returns empty set`() = runTest {
        coEvery { formRepo.getForms() } returns DomainResult.Success(listOf(FormSummary("f1", "Form 1")))
        coEvery { draftRepo.getFormIdsWithDrafts() } returns DomainResult.Success(emptyList())

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
        coEvery { formRepo.getForms() } returns DomainResult.Success(forms)
        coEvery { draftRepo.getFormIdsWithDrafts() } returns DomainResult.Success(listOf("f1", "f3"))

        val vm = createViewModel()

        val drafts = vm.state.value.drafts
        assertEquals(setOf("f1", "f3"), drafts)
        assertFalse(drafts.contains("f2"))
    }

    @Test
    fun `pending submissions are loaded into state`() = runTest {
        coEvery { formRepo.getForms() } returns DomainResult.Success(emptyList())
        coEvery { draftRepo.getFormIdsWithDrafts() } returns DomainResult.Success(emptyList())

        val vm = createViewModel()

        val submission = PendingSubmission(
            id = "sub-1", formId = "f1", formTitle = "Test Form",
            values = emptyMap(), status = SubmissionStatus.PENDING,
            errorMessage = null, attemptCount = 0,
            createdAt = 1000L, updatedAt = 1000L
        )
        pendingFlow.value = listOf(submission)

        assertEquals(1, vm.state.value.pendingSubmissions.size)
        assertEquals("sub-1", vm.state.value.pendingSubmissions[0].id)
        assertEquals(mapOf("f1" to 1), vm.state.value.pendingCountsByFormId)
    }

    @Test
    fun `retry calls repository retry and schedules sync`() = runTest {
        coEvery { formRepo.getForms() } returns DomainResult.Success(emptyList())
        coEvery { draftRepo.getFormIdsWithDrafts() } returns DomainResult.Success(emptyList())

        val vm = createViewModel()
        vm.retrySubmission("sub-1")

        coVerify { submissionQueueRepo.retry("sub-1") }
        verify { syncWorkScheduler.scheduleSync() }
    }

    @Test
    fun `discard calls repository delete`() = runTest {
        coEvery { formRepo.getForms() } returns DomainResult.Success(emptyList())
        coEvery { draftRepo.getFormIdsWithDrafts() } returns DomainResult.Success(emptyList())

        val vm = createViewModel()
        vm.discardSubmission("sub-1")

        coVerify { submissionQueueRepo.delete("sub-1") }
    }
}
