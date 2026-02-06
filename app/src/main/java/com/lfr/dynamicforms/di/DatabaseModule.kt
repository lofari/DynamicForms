package com.lfr.dynamicforms.di

import android.content.Context
import androidx.room.Room
import com.lfr.dynamicforms.data.local.AppDatabase
import com.lfr.dynamicforms.data.local.DraftDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "dynamicforms.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideDraftDao(database: AppDatabase): DraftDao = database.draftDao()
}
