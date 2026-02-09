package com.lfr.dynamicforms.domain.usecase

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
    private val draftRepo = mockk<DraftRepository>(relaxUnitFun = true)
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
    fun `validation errors return ValidationFailed without API call`() = runTest {
        val errors = mapOf("name" to "Required")
        every { validatePage.validateAllPages(form.pages, values) } returns errors
        every { validatePage.firstPageWithErrors(form.pages, errors) } returns 0

        val result = useCase(form, values)

        assertTrue(result is SubmitResult.ValidationFailed)
        val failed = result as SubmitResult.ValidationFailed
        assertEquals(errors, failed.errors)
        coVerify(exactly = 0) { formRepo.submitForm(any(), any(), any()) }
    }

    @Test
    fun `ValidationFailed includes correct first error page`() = runTest {
        val errors = mapOf("email" to "Invalid email")
        every { validatePage.validateAllPages(form.pages, values) } returns errors
        every { validatePage.firstPageWithErrors(form.pages, errors) } returns 1

        val result = useCase(form, values)

        assertTrue(result is SubmitResult.ValidationFailed)
        assertEquals(1, (result as SubmitResult.ValidationFailed).firstErrorPage)
    }

    @Test
    fun `successful submission returns Success and deletes draft`() = runTest {
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns SubmissionResponse(success = true, message = "Done")

        val result = useCase(form, values)

        assertTrue(result is SubmitResult.Success)
        assertEquals("Done", (result as SubmitResult.Success).message)
        coVerify { draftRepo.deleteDraft("f1") }
    }

    @Test
    fun `success with null message uses default`() = runTest {
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns SubmissionResponse(success = true, message = null)

        val result = useCase(form, values)

        assertTrue(result is SubmitResult.Success)
        assertEquals("Form submitted successfully", (result as SubmitResult.Success).message)
    }

    @Test
    fun `server error returns ServerError with field errors`() = runTest {
        val fieldErrors = mapOf("name" to "too short")
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns SubmissionResponse(
            success = false,
            fieldErrors = fieldErrors
        )
        every { validatePage.firstPageWithErrors(form.pages, fieldErrors) } returns 0

        val result = useCase(form, values)

        assertTrue(result is SubmitResult.ServerError)
        val serverError = result as SubmitResult.ServerError
        assertEquals(fieldErrors, serverError.fieldErrors)
    }

    @Test
    fun `server error includes first error page`() = runTest {
        val fieldErrors = mapOf("email" to "already taken")
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns SubmissionResponse(
            success = false,
            fieldErrors = fieldErrors
        )
        every { validatePage.firstPageWithErrors(form.pages, fieldErrors) } returns 1

        val result = useCase(form, values)

        assertTrue(result is SubmitResult.ServerError)
        assertEquals(1, (result as SubmitResult.ServerError).firstErrorPage)
    }

    @Test
    fun `network exception returns NetworkError`() = runTest {
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } throws RuntimeException("Timeout")

        val result = useCase(form, values)

        assertTrue(result is SubmitResult.NetworkError)
        assertEquals("Timeout", (result as SubmitResult.NetworkError).exception.message)
        coVerify(exactly = 0) { draftRepo.deleteDraft(any()) }
    }

    @Test
    fun `draft not deleted on server error`() = runTest {
        val fieldErrors = mapOf("name" to "too short")
        every { validatePage.validateAllPages(form.pages, values) } returns emptyMap()
        coEvery { formRepo.submitForm("f1", values, any()) } returns SubmissionResponse(
            success = false,
            fieldErrors = fieldErrors
        )
        every { validatePage.firstPageWithErrors(form.pages, fieldErrors) } returns 0

        useCase(form, values)

        coVerify(exactly = 0) { draftRepo.deleteDraft(any()) }
    }
}
