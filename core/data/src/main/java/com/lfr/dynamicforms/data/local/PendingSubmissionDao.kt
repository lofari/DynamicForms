package com.lfr.dynamicforms.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSubmissionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingSubmissionEntity)

    @Query("SELECT * FROM pending_submissions ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PendingSubmissionEntity>>

    @Query("SELECT COUNT(*) FROM pending_submissions WHERE formId = :formId AND status != 'SYNCING'")
    fun observePendingCountByFormId(formId: String): Flow<Int>

    @Query("SELECT * FROM pending_submissions WHERE status = 'PENDING'")
    suspend fun getPending(): List<PendingSubmissionEntity>

    @Query(
        "UPDATE pending_submissions " +
            "SET status = :status, errorMessage = :errorMessage, " +
            "attemptCount = :attemptCount, updatedAt = :updatedAt " +
            "WHERE id = :id"
    )
    suspend fun updateStatus(
        id: String,
        status: String,
        errorMessage: String?,
        attemptCount: Int,
        updatedAt: Long,
    )

    @Query("DELETE FROM pending_submissions WHERE id = :id")
    suspend fun delete(id: String)
}
