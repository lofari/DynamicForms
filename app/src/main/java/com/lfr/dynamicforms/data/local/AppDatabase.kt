package com.lfr.dynamicforms.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DraftEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun draftDao(): DraftDao
}
