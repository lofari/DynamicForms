package com.lfr.dynamicforms.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val formId: String,
    val pageIndex: Int,
    val valuesJson: String,
    val updatedAt: Long
)
