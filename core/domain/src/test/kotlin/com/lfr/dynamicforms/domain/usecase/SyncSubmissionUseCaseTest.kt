package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import com.lfr.dynamicforms.domain.model.SubmissionStatus
import com.lfr.dynamicforms.domain.repository.FormRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncSubmissionUseCaseTest {

    private val submissionQueueRepo = mockk<SubmissionQueueRepository>(relaxUnitFun = true)
    private val formRepo = mockk<FormRepository>()
    private val useCase = SyncSubmissionUseCase(submissionQueueRepo, formRepo)

    private fun submission(attemptCount: Int = 0) = PendingSubmission(
        id = "sub-1",
        formId = "f1",
        formTitle = "Test Form",
        values = mapOf("name" to "Jane"),
        status = SubmissionStatus.PENDING,
        errorMessage = null,
        attemptCount = attemptCount,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Test
    fun `sets SYNCING before API call`() = runTest {
        coEvery { formRepo.submitForm("f1", any(), idempotencyKey = "sub-1") } returns
                SubmissionResponse(success = true, message = "OK")

        useCase(submission())

        coVerify { submissionQueueRepo.updateStatus("sub-1", SubmissionStatus.SYNCING) }
    }

    @Test
    fun `success deletes from queue and returns Synced`() = runTest {
        coEvery { formRepo.submitForm("f1", any(), idempotencyKey = "sub-1") } returns
                SubmissionResponse(success = true, message = "OK")

        val result = useCase(submission())

        assertEquals(SyncResult.Synced, result)
        coVerify { submissionQueueRepo.delete("sub-1") }
    }

    @Test
    fun `server validation error marks FAILED and returns Failed`() = runTest {
        coEvery { formRepo.submitForm("f1", any(), idempotencyKey = "sub-1") } returns
                SubmissionResponse(success = false, message = "Validation failed", fieldErrors = mapOf("name" to "too short"))

        val result = useCase(submission())

        assertEquals(SyncResult.Failed, result)
        coVerify {
            submissionQueueRepo.updateStatus(
                "sub-1",
                SubmissionStatus.FAILED,
                errorMessage = "Validation failed",
                attemptCount = 1
            )
        }
    }

    @Test
    fun `network exception with attempt less than 5 stays PENDING and returns Retryable`() = runTest {
        coEvery { formRepo.submitForm("f1", any(), idempotencyKey = "sub-1") } throws RuntimeException("Timeout")

        val result = useCase(submission(attemptCount = 2))

        assertEquals(SyncResult.Retryable, result)
        coVerify {
            submissionQueueRepo.updateStatus(
                "sub-1",
                SubmissionStatus.PENDING,
                attemptCount = 3
            )
        }
    }

    @Test
    fun `network exception at attempt 4 (becomes 5) marks FAILED`() = runTest {
        coEvery { formRepo.submitForm("f1", any(), idempotencyKey = "sub-1") } throws RuntimeException("Timeout")

        val result = useCase(submission(attemptCount = 4))

        assertEquals(SyncResult.Failed, result)
        coVerify {
            submissionQueueRepo.updateStatus(
                "sub-1",
                SubmissionStatus.FAILED,
                errorMessage = "Timeout",
                attemptCount = 5
            )
        }
    }
}
