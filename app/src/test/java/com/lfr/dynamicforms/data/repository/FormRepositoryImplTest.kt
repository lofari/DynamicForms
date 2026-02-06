package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.remote.FormApi
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
import org.junit.Test

class FormRepositoryImplTest {

    private val api = mockk<FormApi>(relaxUnitFun = true)
    private val repository = FormRepositoryImpl(api)

    @Test
    fun `getForms delegates to api`() = runTest {
        val expected = listOf(
            FormSummary(formId = "f1", title = "Form One"),
            FormSummary(formId = "f2", title = "Form Two")
        )
        coEvery { api.getForms() } returns expected

        val result = repository.getForms()

        assertEquals(expected, result)
        coVerify { api.getForms() }
    }

    @Test
    fun `getForm passes formId to api`() = runTest {
        val expected = Form(formId = "f1", title = "Form One", pages = emptyList())
        coEvery { api.getForm("f1") } returns expected

        val result = repository.getForm("f1")

        assertEquals(expected, result)
        coVerify { api.getForm("f1") }
    }

    @Test
    fun `submitForm wraps values in FormSubmission`() = runTest {
        val submissionSlot = slot<FormSubmission>()
        coEvery { api.submitForm(any(), capture(submissionSlot)) } returns SubmissionResponse(
            success = true,
            message = "OK"
        )

        repository.submitForm("f1", mapOf("k" to "v"))

        coVerify { api.submitForm("f1", any()) }
        val captured = submissionSlot.captured
        assertEquals("f1", captured.formId)
        assertEquals(mapOf("k" to "v"), captured.values)
    }

    @Test
    fun `submitForm returns response from api`() = runTest {
        val expected = SubmissionResponse(success = true, message = "Done")
        coEvery { api.submitForm(any(), any()) } returns expected

        val result = repository.submitForm("f1", mapOf("k" to "v"))

        assertEquals(expected, result)
    }

    @Test(expected = RuntimeException::class)
    fun `getForm propagates exceptions`() = runTest {
        coEvery { api.getForm("x") } throws RuntimeException("fail")

        repository.getForm("x")
    }
}
