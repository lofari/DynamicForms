package com.lfr.dynamicforms.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_submissions")
data class PendingSubmissionEntity(
    @PrimaryKey val id: String,
    val formId: String,
    val formTitle: String,
    val valuesJson: String,
    val status: String,
    val errorMessage: String?,
    val attemptCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)
