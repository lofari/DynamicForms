package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.local.PendingSubmissionDao
import com.lfr.dynamicforms.data.local.PendingSubmissionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SubmissionQueueRepositoryImplTest {

    private val dao = mockk<PendingSubmissionDao>(relaxUnitFun = true)
    private val json = Json { ignoreUnknownKeys = true }
    private val repository = SubmissionQueueRepositoryImpl(dao, json)

    @Test
    fun `enqueue creates entity with UUID and PENDING status`() = runTest {
        val entitySlot = slot<PendingSubmissionEntity>()
        coEvery { dao.upsert(capture(entitySlot)) } returns Unit

        val id = repository.enqueue("f1", "Test Form", mapOf("name" to "Jane"))

        assertNotNull(id)
        val captured = entitySlot.captured
        assertEquals(id, captured.id)
        assertEquals("f1", captured.formId)
        assertEquals("Test Form", captured.formTitle)
        assertEquals("PENDING", captured.status)
        assertEquals(0, captured.attemptCount)
        val values: Map<String, String> = Json.decodeFromString(captured.valuesJson)
        assertEquals(mapOf("name" to "Jane"), values)
    }

    @Test
    fun `retry resets status and attemptCount`() = runTest {
        repository.retry("sub-1")

        coVerify {
            dao.updateStatus(
                id = "sub-1",
                status = "PENDING",
                errorMessage = null,
                attemptCount = 0,
                updatedAt = any()
            )
        }
    }

    @Test
    fun `delete removes entity`() = runTest {
        repository.delete("sub-1")

        coVerify { dao.delete("sub-1") }
    }
}
