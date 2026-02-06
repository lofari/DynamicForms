package com.lfr.dynamicforms.model

import kotlinx.serialization.Serializable

@Serializable
data class Submission(
    val id: String,
    val formId: String,
    val values: Map<String, String>,
    val submittedAt: Long
)

@Serializable
data class SubmissionRequest(
    val formId: String,
    val values: Map<String, String>
)
