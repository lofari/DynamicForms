package com.lfr.dynamicforms.domain.model

enum class SubmissionStatus { PENDING, SYNCING, FAILED }

data class PendingSubmission(
    val id: String,
    val formId: String,
    val formTitle: String,
    val values: Map<String, String>,
    val status: SubmissionStatus,
    val errorMessage: String?,
    val attemptCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)
