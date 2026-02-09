package com.lfr.dynamicforms.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: DraftEntity)

    @Query("SELECT * FROM drafts WHERE formId = :formId")
    suspend fun getDraft(formId: String): DraftEntity?

    @Query("DELETE FROM drafts WHERE formId = :formId")
    suspend fun deleteDraft(formId: String)

    @Query("SELECT formId FROM drafts")
    suspend fun getFormIdsWithDrafts(): List<String>
}
