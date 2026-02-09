package com.lfr.dynamicforms.presentation.util

import com.lfr.dynamicforms.domain.model.DomainError

fun DomainError.toUserMessage(): String = when (this) {
    is DomainError.Network -> "No internet connection. Check your network and try again."
    is DomainError.Timeout -> "Request timed out. Please try again."
    is DomainError.NotFound -> "Form not found."
    is DomainError.Server -> "Server error. Please try again later."
    is DomainError.Validation -> "Please fix the errors below."
    is DomainError.Storage -> "Could not save data. Please try again."
    is DomainError.Unknown -> "Something went wrong. Please try again."
}
