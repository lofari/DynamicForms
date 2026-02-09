package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import com.lfr.dynamicforms.domain.repository.DraftRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveDraftUseCaseTest {

    private val draftRepo = mockk<DraftRepository>()
    private val useCase = SaveDraftUseCase(draftRepo)

    @Test
    fun `delegates to repository with correct parameters`() = runTest {
        val values = mapOf("name" to "Alice", "email" to "alice@test.com")
        coEvery { draftRepo.saveDraft("f1", 2, values) } returns DomainResult.Success(Unit)

        val result = useCase("f1", 2, values)

        assertTrue(result is DomainResult.Success)
        coVerify { draftRepo.saveDraft("f1", 2, values) }
    }

    @Test
    fun `propagates repository failure`() = runTest {
        coEvery { draftRepo.saveDraft(any(), any(), any()) } returns DomainResult.Failure(DomainError.Storage())

        val result = useCase("f1", 0, emptyMap())

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Storage)
    }
}
