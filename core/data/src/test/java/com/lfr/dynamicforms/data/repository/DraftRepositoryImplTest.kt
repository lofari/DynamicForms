package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.local.DraftDao
import com.lfr.dynamicforms.data.local.DraftEntity
import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftRepositoryImplTest {

    private val draftDao = mockk<DraftDao>(relaxUnitFun = true)
    private val json = Json { ignoreUnknownKeys = true }
    private val repository = DraftRepositoryImpl(draftDao, json)

    @Test
    fun `saveDraft serializes values to JSON entity`() = runTest {
        val entitySlot = slot<DraftEntity>()
        coEvery { draftDao.upsert(capture(entitySlot)) } returns Unit

        val result = repository.saveDraft("f1", 2, mapOf("name" to "Jane"))

        assertTrue(result is DomainResult.Success)
        coVerify { draftDao.upsert(any()) }
        val captured = entitySlot.captured
        assertEquals("f1", captured.formId)
        assertEquals(2, captured.pageIndex)
        val deserializedValues = Json.decodeFromString<Map<String, String>>(captured.valuesJson)
        assertEquals(mapOf("name" to "Jane"), deserializedValues)
    }

    @Test
    fun `getDraft deserializes entity to Draft`() = runTest {
        val entity = DraftEntity(
            formId = "f1",
            pageIndex = 2,
            valuesJson = """{"name":"Jane"}""",
            updatedAt = 1000L
        )
        coEvery { draftDao.getDraft("f1") } returns entity

        val result = repository.getDraft("f1")

        assertTrue(result is DomainResult.Success)
        val draft = (result as DomainResult.Success).data!!
        assertEquals("f1", draft.formId)
        assertEquals(2, draft.pageIndex)
        assertEquals(mapOf("name" to "Jane"), draft.values)
        assertEquals(1000L, draft.updatedAt)
    }

    @Test
    fun `getDraft returns Success null when no entity`() = runTest {
        coEvery { draftDao.getDraft("f1") } returns null

        val result = repository.getDraft("f1")

        assertTrue(result is DomainResult.Success)
        assertNull((result as DomainResult.Success).data)
    }

    @Test
    fun `getDraft returns Success null and deletes draft when JSON is corrupted`() = runTest {
        val entity = DraftEntity(
            formId = "f1",
            pageIndex = 0,
            valuesJson = "not valid json {{{",
            updatedAt = 1000L
        )
        coEvery { draftDao.getDraft("f1") } returns entity

        val result = repository.getDraft("f1")

        assertTrue(result is DomainResult.Success)
        assertNull((result as DomainResult.Success).data)
        coVerify { draftDao.deleteDraft("f1") }
    }

    @Test
    fun `saveDraft returns Failure with Storage error on exception`() = runTest {
        coEvery { draftDao.upsert(any()) } throws RuntimeException("DB error")

        val result = repository.saveDraft("f1", 0, emptyMap())

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Storage)
    }
}
