package com.lfr.dynamicforms.di

import com.lfr.dynamicforms.data.repository.DraftRepositoryImpl
import com.lfr.dynamicforms.data.repository.FormRepositoryImpl
import com.lfr.dynamicforms.data.repository.SubmissionQueueRepositoryImpl
import com.lfr.dynamicforms.data.worker.SyncWorkSchedulerImpl
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import com.lfr.dynamicforms.domain.usecase.SyncWorkScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFormRepository(impl: FormRepositoryImpl): FormRepository

    @Binds
    @Singleton
    abstract fun bindDraftRepository(impl: DraftRepositoryImpl): DraftRepository

    @Binds
    @Singleton
    abstract fun bindSubmissionQueueRepository(impl: SubmissionQueueRepositoryImpl): SubmissionQueueRepository

    @Binds
    @Singleton
    abstract fun bindSyncWorkScheduler(impl: SyncWorkSchedulerImpl): SyncWorkScheduler
}
