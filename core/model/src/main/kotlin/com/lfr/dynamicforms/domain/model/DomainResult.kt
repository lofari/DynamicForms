package com.lfr.dynamicforms.domain.model

/**
 * A typed result wrapper for domain operations, replacing raw exception propagation
 * with explicit success/failure in the type signature.
 */
sealed interface DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>
    data class Failure(val error: Throwable) : DomainResult<Nothing>
}

inline fun <T, R> DomainResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R
): R = when (this) {
    is DomainResult.Success -> onSuccess(data)
    is DomainResult.Failure -> onFailure(error)
}
