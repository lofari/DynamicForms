package com.lfr.dynamicforms.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Form(
    val formId: String,
    val title: String,
    val pages: List<Page>
)

@Serializable
data class Page(
    val pageId: String,
    val title: String,
    val elements: List<FormElement>
)

@Serializable
data class FormSummary(
    val formId: String,
    val title: String,
    val description: String = "",
    val pageCount: Int = 0,
    val fieldCount: Int = 0
)

@Serializable
data class FormSubmission(
    val formId: String,
    val values: Map<String, String>
)

@Serializable
data class SubmissionResponse(
    val success: Boolean,
    val message: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)

data class Draft(
    val formId: String,
    val pageIndex: Int,
    val values: Map<String, String>,
    val updatedAt: Long
)
