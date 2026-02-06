package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.local.DraftDao
import com.lfr.dynamicforms.data.local.DraftEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DraftRepositoryImplTest {

    private val draftDao = mockk<DraftDao>(relaxUnitFun = true)
    private val repository = DraftRepositoryImpl(draftDao)

    @Test
    fun `saveDraft serializes values to JSON entity`() = runTest {
        val entitySlot = slot<DraftEntity>()
        coEvery { draftDao.upsert(capture(entitySlot)) } returns Unit

        repository.saveDraft("f1", 2, mapOf("name" to "Jane"))

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

        assertEquals("f1", result!!.formId)
        assertEquals(2, result.pageIndex)
        assertEquals(mapOf("name" to "Jane"), result.values)
        assertEquals(1000L, result.updatedAt)
    }

    @Test
    fun `getDraft returns null when no entity`() = runTest {
        coEvery { draftDao.getDraft("f1") } returns null

        val result = repository.getDraft("f1")

        assertNull(result)
    }
}
