package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.remote.FormApi
import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormSubmission
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class FormRepositoryImplTest {

    private val api = mockk<FormApi>(relaxUnitFun = true)
    private val repository = FormRepositoryImpl(api)

    @Test
    fun `getForms returns Success with forms`() = runTest {
        val expected = listOf(
            FormSummary(formId = "f1", title = "Form One"),
            FormSummary(formId = "f2", title = "Form Two")
        )
        coEvery { api.getForms() } returns expected

        val result = repository.getForms()

        assertTrue(result is DomainResult.Success)
        assertEquals(expected, (result as DomainResult.Success).data)
    }

    @Test
    fun `getForm returns Success with form`() = runTest {
        val expected = Form(formId = "f1", title = "Form One", pages = emptyList())
        coEvery { api.getForm("f1") } returns expected

        val result = repository.getForm("f1")

        assertTrue(result is DomainResult.Success)
        assertEquals(expected, (result as DomainResult.Success).data)
    }

    @Test
    fun `submitForm wraps values in FormSubmission`() = runTest {
        val submissionSlot = slot<FormSubmission>()
        coEvery { api.submitForm(any(), capture(submissionSlot), any()) } returns SubmissionResponse(
            success = true,
            message = "OK"
        )

        val result = repository.submitForm("f1", mapOf("k" to "v"))

        assertTrue(result is DomainResult.Success)
        coVerify { api.submitForm("f1", any(), any()) }
        val captured = submissionSlot.captured
        assertEquals("f1", captured.formId)
        assertEquals(mapOf("k" to "v"), captured.values)
    }

    @Test
    fun `submitForm passes idempotency key to api`() = runTest {
        coEvery { api.submitForm(any(), any(), any()) } returns SubmissionResponse(
            success = true,
            message = "OK"
        )

        repository.submitForm("f1", mapOf("k" to "v"), idempotencyKey = "key-123")

        coVerify { api.submitForm("f1", any(), "key-123") }
    }

    @Test
    fun `getForm returns Failure with Network error on UnknownHostException`() = runTest {
        coEvery { api.getForm("x") } throws UnknownHostException("no host")

        val result = repository.getForm("x")

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Network)
    }

    @Test
    fun `getForm returns Failure with Unknown error on RuntimeException`() = runTest {
        coEvery { api.getForm("x") } throws RuntimeException("fail")

        val result = repository.getForm("x")

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Unknown)
    }
}
