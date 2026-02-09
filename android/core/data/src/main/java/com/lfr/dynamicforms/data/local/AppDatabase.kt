package com.lfr.dynamicforms.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DraftEntity::class, PendingSubmissionEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun draftDao(): DraftDao
    abstract fun pendingSubmissionDao(): PendingSubmissionDao
}
