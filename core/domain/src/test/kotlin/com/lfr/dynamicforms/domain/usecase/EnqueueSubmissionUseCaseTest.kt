package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnqueueSubmissionUseCaseTest {

    private val submissionQueueRepo = mockk<SubmissionQueueRepository>()
    private val draftRepo = mockk<DraftRepository>()
    private val workScheduler = mockk<SyncWorkScheduler>(relaxUnitFun = true)
    private val useCase = EnqueueSubmissionUseCase(submissionQueueRepo, draftRepo, workScheduler)

    @Test
    fun `enqueues to repository and returns submission ID`() = runTest {
        coEvery { submissionQueueRepo.enqueue("f1", "Test Form", any()) } returns DomainResult.Success("uuid-123")
        coEvery { draftRepo.deleteDraft("f1") } returns DomainResult.Success(Unit)

        val result = useCase("f1", "Test Form", mapOf("name" to "Jane"))

        assertTrue(result is DomainResult.Success)
        assertEquals("uuid-123", (result as DomainResult.Success).data)
        coVerify { submissionQueueRepo.enqueue("f1", "Test Form", mapOf("name" to "Jane")) }
    }

    @Test
    fun `deletes draft after enqueuing`() = runTest {
        coEvery { submissionQueueRepo.enqueue(any(), any(), any()) } returns DomainResult.Success("uuid-123")
        coEvery { draftRepo.deleteDraft("f1") } returns DomainResult.Success(Unit)

        useCase("f1", "Test Form", mapOf("name" to "Jane"))

        coVerify { draftRepo.deleteDraft("f1") }
    }

    @Test
    fun `schedules sync worker after enqueuing`() = runTest {
        coEvery { submissionQueueRepo.enqueue(any(), any(), any()) } returns DomainResult.Success("uuid-123")
        coEvery { draftRepo.deleteDraft("f1") } returns DomainResult.Success(Unit)

        useCase("f1", "Test Form", mapOf("name" to "Jane"))

        verify { workScheduler.scheduleSync() }
    }

    @Test
    fun `enqueue failure propagates without deleting draft or scheduling`() = runTest {
        coEvery { submissionQueueRepo.enqueue(any(), any(), any()) } returns DomainResult.Failure(DomainError.Storage())

        val result = useCase("f1", "Test Form", mapOf("name" to "Jane"))

        assertTrue(result is DomainResult.Failure)
        coVerify(exactly = 0) { draftRepo.deleteDraft(any()) }
        verify(exactly = 0) { workScheduler.scheduleSync() }
    }
}
