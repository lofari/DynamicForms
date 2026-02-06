package com.lfr.dynamicforms.presentation.form

import com.lfr.dynamicforms.domain.model.Form

data class FormUiState(
    val isLoading: Boolean = true,
    val formId: String? = null,
    val form: Form? = null,
    val currentPageIndex: Int = 0,
    val values: Map<String, String> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
    val repeatingGroupCounts: Map<String, Int> = emptyMap(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val totalPages: Int get() = form?.pages?.size ?: 0
    val isFirstPage: Boolean get() = currentPageIndex == 0
    val isLastPage: Boolean get() = currentPageIndex == totalPages - 1
    val currentPage get() = form?.pages?.getOrNull(currentPageIndex)
    val progressFraction: Float get() = if (totalPages > 0) (currentPageIndex + 1f) / totalPages else 0f
}
