package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.model.Draft
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.Page
import com.lfr.dynamicforms.domain.model.TextFieldElement
import com.lfr.dynamicforms.domain.model.ToggleElement
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetFormUseCaseTest {

    private val formRepo = mockk<FormRepository>()
    private val draftRepo = mockk<DraftRepository>()
    private val useCase = GetFormUseCase(formRepo, draftRepo)

    @Test
    fun `returns form with default values when no draft`() = runTest {
        val toggle = ToggleElement(id = "toggle1", label = "Accept", defaultValue = true)
        val textField = TextFieldElement(id = "text1", label = "Name")
        val form = Form(
            formId = "f1",
            title = "Test Form",
            pages = listOf(Page(pageId = "p1", title = "Page 1", elements = listOf(toggle, textField)))
        )

        coEvery { formRepo.getForm("f1") } returns DomainResult.Success(form)
        coEvery { draftRepo.getDraft("f1") } returns DomainResult.Success(null)

        val result = useCase("f1")

        assertTrue(result is DomainResult.Success)
        val data = (result as DomainResult.Success).data
        assertEquals(form, data.form)
        assertEquals("true", data.initialValues["toggle1"])
        assertTrue("text1" !in data.initialValues)
        assertEquals(0, data.initialPageIndex)
    }

    @Test
    fun `draft values override defaults`() = runTest {
        val toggle = ToggleElement(id = "toggle1", label = "Accept", defaultValue = true)
        val form = Form(
            formId = "f1",
            title = "Test Form",
            pages = listOf(Page(pageId = "p1", title = "Page 1", elements = listOf(toggle)))
        )
        val draft = Draft(
            formId = "f1",
            pageIndex = 0,
            values = mapOf("toggle1" to "false"),
            updatedAt = 1000L
        )

        coEvery { formRepo.getForm("f1") } returns DomainResult.Success(form)
        coEvery { draftRepo.getDraft("f1") } returns DomainResult.Success(draft)

        val result = useCase("f1") as DomainResult.Success

        assertEquals("false", result.data.initialValues["toggle1"])
    }

    @Test
    fun `draft restores page index`() = runTest {
        val form = Form(
            formId = "f1",
            title = "Test Form",
            pages = listOf(Page(pageId = "p1", title = "Page 1", elements = emptyList()))
        )
        val draft = Draft(
            formId = "f1",
            pageIndex = 2,
            values = emptyMap(),
            updatedAt = 1000L
        )

        coEvery { formRepo.getForm("f1") } returns DomainResult.Success(form)
        coEvery { draftRepo.getDraft("f1") } returns DomainResult.Success(draft)

        val result = useCase("f1") as DomainResult.Success

        assertEquals(2, result.data.initialPageIndex)
    }

    @Test
    fun `non-defaultable elements excluded from defaults`() = runTest {
        val textField = TextFieldElement(id = "text1", label = "Name")
        val form = Form(
            formId = "f1",
            title = "Test Form",
            pages = listOf(Page(pageId = "p1", title = "Page 1", elements = listOf(textField)))
        )

        coEvery { formRepo.getForm("f1") } returns DomainResult.Success(form)
        coEvery { draftRepo.getDraft("f1") } returns DomainResult.Success(null)

        val result = useCase("f1") as DomainResult.Success

        assertTrue(result.data.initialValues.isEmpty())
    }

    @Test
    fun `form repository failure propagates as Failure`() = runTest {
        coEvery { formRepo.getForm("f1") } returns DomainResult.Failure(DomainError.Network())

        val result = useCase("f1")

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Network)
    }

    @Test
    fun `draft repository failure propagates as Failure`() = runTest {
        val form = Form(formId = "f1", title = "Test", pages = emptyList())
        coEvery { formRepo.getForm("f1") } returns DomainResult.Success(form)
        coEvery { draftRepo.getDraft("f1") } returns DomainResult.Failure(DomainError.Storage())

        val result = useCase("f1")

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Storage)
    }
}
