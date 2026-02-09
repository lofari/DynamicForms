package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.Page
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import com.lfr.dynamicforms.domain.model.TextFieldElement
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitFormUseCaseTest {

    private val formRepo = mockk<FormRepository>()
    private val draftRepo = mockk<DraftRepository>()
    private val validatePage = mockk<ValidatePageUseCase>()
    private val useCase = SubmitFormUseCase(formRepo, draftRepo, validatePage)

    private val form = Form(
        formId = "f1",
        title = "Test Form",
        pages = listOf(
            Page(pageId = "p1", title = "Page 1", elements = listOf(TextFieldElement(id = "name", label = "Name"))),
            Page(pageId = "p2", title = "Page 2", elements = listOf(TextFieldElement(id = "email", label = "Email")))
        )
    )

    private val values = mapOf("name" to "Jane", "email" to "jane@test.com")

    @Test
    fun `validation errors return Failure with Validation error`() = runTest {
        val errors = mapOf("name" to "Required")
        every { validatePage.validateAllPages(form.pages, values) } returns errors

        val result = useCase(form, values)

        assertTrue(result is DomainResult.Failure)
        val error = (result as DomainResult.Failure).error
        assertTrue(error is DomainError.Validation)
        assertEquals(errors, (error as DomainError.Validation).fieldErrors)
        coVerify(exactly = 0) { formRepo.submitForm(any(), any(), any()) }
    }

    @Test
    fun `successful submission returns Success and deletes draft`() = runTest {
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns DomainResult.Success(
            SubmissionResponse(success = true, message = "Done")
        )
        coEvery { draftRepo.deleteDraft("f1") } returns DomainResult.Success(Unit)

        val result = useCase(form, values)

        assertTrue(result is DomainResult.Success)
        val response = (result as DomainResult.Success).data
        assertTrue(response.success)
        assertEquals("Done", response.message)
        coVerify { draftRepo.deleteDraft("f1") }
    }

    @Test
    fun `server error returns Success with fieldErrors`() = runTest {
        val fieldErrors = mapOf("name" to "too short")
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns DomainResult.Success(
            SubmissionResponse(success = false, fieldErrors = fieldErrors)
        )

        val result = useCase(form, values)

        assertTrue(result is DomainResult.Success)
        val response = (result as DomainResult.Success).data
        assertEquals(false, response.success)
        assertEquals(fieldErrors, response.fieldErrors)
    }

    @Test
    fun `network error from repo propagates as Failure`() = runTest {
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns DomainResult.Failure(DomainError.Network())

        val result = useCase(form, values)

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Network)
        coVerify(exactly = 0) { draftRepo.deleteDraft(any()) }
    }

    @Test
    fun `draft not deleted on server error`() = runTest {
        val fieldErrors = mapOf("name" to "too short")
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns DomainResult.Success(
            SubmissionResponse(success = false, fieldErrors = fieldErrors)
        )

        useCase(form, values)

        coVerify(exactly = 0) { draftRepo.deleteDraft(any()) }
    }
}
