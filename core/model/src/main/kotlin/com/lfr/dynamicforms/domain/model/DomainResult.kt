package com.lfr.dynamicforms.domain.model

/**
 * Structured error types for domain operations.
 */
sealed class DomainError {
    data class Network(val cause: Throwable? = null) : DomainError()
    data class Timeout(val cause: Throwable? = null) : DomainError()
    data object NotFound : DomainError()
    data class Server(val code: Int? = null, val message: String? = null) : DomainError()
    data class Validation(val fieldErrors: Map<String, String>) : DomainError()
    data class Storage(val cause: Throwable? = null) : DomainError()
    data class Unknown(val cause: Throwable? = null) : DomainError()
}

/**
 * A typed result wrapper for domain operations, replacing raw exception propagation
 * with explicit success/failure in the type signature.
 */
sealed interface DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>
}

inline fun <T, R> DomainResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (DomainError) -> R
): R = when (this) {
    is DomainResult.Success -> onSuccess(data)
    is DomainResult.Failure -> onFailure(error)
}

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> =
    when (this) {
        is DomainResult.Success -> DomainResult.Success(transform(data))
        is DomainResult.Failure -> this
    }

suspend inline fun <T, R> DomainResult<T>.flatMap(
    crossinline transform: suspend (T) -> DomainResult<R>
): DomainResult<R> =
    when (this) {
        is DomainResult.Success -> transform(data)
        is DomainResult.Failure -> this
    }
