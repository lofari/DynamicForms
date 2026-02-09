package com.lfr.dynamicforms.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesDrafts() {
        // Create v1 database with a draft
        helper.createDatabase("test-db", 1).apply {
            execSQL(
                """
                INSERT INTO drafts (formId, pageIndex, valuesJson, updatedAt)
                VALUES ('f1', 0, '{"name":"Jane"}', 1000)
                """.trimIndent()
            )
            close()
        }

        // Run migration 1→2
        val db = helper.runMigrationsAndValidate("test-db", 2, true, MIGRATION_1_2)

        // Verify draft is still there
        val cursor = db.query("SELECT formId, valuesJson FROM drafts")
        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertEquals("f1", cursor.getString(0))
        assertEquals("{\"name\":\"Jane\"}", cursor.getString(1))
        cursor.close()

        // Verify pending_submissions table was created
        val tableCursor = db.query("SELECT * FROM pending_submissions")
        assertEquals(0, tableCursor.count)
        tableCursor.close()

        db.close()
    }

    @Test
    fun migrate1To2_pendingSubmissionsTableIsUsable() {
        helper.createDatabase("test-db-2", 1).close()

        val db = helper.runMigrationsAndValidate("test-db-2", 2, true, MIGRATION_1_2)

        // Insert a pending submission to verify the table schema is correct
        db.execSQL(
            """
            INSERT INTO pending_submissions (id, formId, formTitle, valuesJson, status, errorMessage, attemptCount, createdAt, updatedAt)
            VALUES ('sub-1', 'f1', 'Test Form', '{}', 'PENDING', NULL, 0, 1000, 1000)
            """.trimIndent()
        )

        val cursor = db.query("SELECT id, formId, status FROM pending_submissions")
        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertEquals("sub-1", cursor.getString(0))
        assertEquals("f1", cursor.getString(1))
        assertEquals("PENDING", cursor.getString(2))
        cursor.close()

        db.close()
    }
}
