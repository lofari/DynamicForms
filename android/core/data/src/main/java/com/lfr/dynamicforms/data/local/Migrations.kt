package com.lfr.dynamicforms.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_submissions` (
                `id` TEXT NOT NULL,
                `formId` TEXT NOT NULL,
                `formTitle` TEXT NOT NULL,
                `valuesJson` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `errorMessage` TEXT,
                `attemptCount` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}
