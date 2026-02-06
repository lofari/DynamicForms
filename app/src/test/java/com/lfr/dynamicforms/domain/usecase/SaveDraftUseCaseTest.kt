package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.repository.DraftRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveDraftUseCaseTest {

    private val draftRepo = mockk<DraftRepository>(relaxUnitFun = true)
    private val useCase = SaveDraftUseCase(draftRepo)

    @Test
    fun `delegates to repository with correct parameters`() = runTest {
        val values = mapOf("name" to "Alice", "email" to "alice@test.com")

        useCase("f1", 2, values)

        coVerify { draftRepo.saveDraft("f1", 2, values) }
    }

    @Test
    fun `propagates repository exception`() = runTest {
        coEvery { draftRepo.saveDraft(any(), any(), any()) } throws RuntimeException("DB error")

        val result = runCatching { useCase("f1", 0, emptyMap()) }

        assertTrue(result.isFailure)
        assertEquals("DB error", result.exceptionOrNull()?.message)
    }
}
