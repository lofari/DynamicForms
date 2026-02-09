package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EnqueueSubmissionUseCaseTest {

    private val submissionQueueRepo = mockk<SubmissionQueueRepository>()
    private val draftRepo = mockk<DraftRepository>(relaxUnitFun = true)
    private val workScheduler = mockk<SyncWorkScheduler>(relaxUnitFun = true)
    private val useCase = EnqueueSubmissionUseCase(submissionQueueRepo, draftRepo, workScheduler)

    @Test
    fun `enqueues to repository and returns submission ID`() = runTest {
        coEvery { submissionQueueRepo.enqueue("f1", "Test Form", any()) } returns "uuid-123"

        val result = useCase("f1", "Test Form", mapOf("name" to "Jane"))

        assertEquals("uuid-123", result)
        coVerify { submissionQueueRepo.enqueue("f1", "Test Form", mapOf("name" to "Jane")) }
    }

    @Test
    fun `deletes draft after enqueuing`() = runTest {
        coEvery { submissionQueueRepo.enqueue(any(), any(), any()) } returns "uuid-123"

        useCase("f1", "Test Form", mapOf("name" to "Jane"))

        coVerify { draftRepo.deleteDraft("f1") }
    }

    @Test
    fun `schedules sync worker after enqueuing`() = runTest {
        coEvery { submissionQueueRepo.enqueue(any(), any(), any()) } returns "uuid-123"

        useCase("f1", "Test Form", mapOf("name" to "Jane"))

        verify { workScheduler.scheduleSync() }
    }
}
