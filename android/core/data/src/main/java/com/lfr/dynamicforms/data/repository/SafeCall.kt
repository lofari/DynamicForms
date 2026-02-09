package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.domain.model.DomainError
import com.lfr.dynamicforms.domain.model.DomainResult
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

@PublishedApi
internal const val HTTP_NOT_FOUND = 404

suspend inline fun <T> safeCall(crossinline block: suspend () -> T): DomainResult<T> =
    try {
        DomainResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: UnknownHostException) {
        Timber.e(e, "DomainError: Network")
        DomainResult.Failure(DomainError.Network(e))
    } catch (e: SocketTimeoutException) {
        Timber.e(e, "DomainError: Timeout")
        DomainResult.Failure(DomainError.Timeout(e))
    } catch (e: HttpException) {
        val error = when (e.code()) {
            HTTP_NOT_FOUND -> DomainError.NotFound
            else -> DomainError.Server(e.code(), e.message())
        }
        Timber.e(e, "DomainError: %s", error::class.simpleName)
        DomainResult.Failure(error)
    } catch (e: Throwable) {
        Timber.e(e, "DomainError: Unknown")
        DomainResult.Failure(DomainError.Unknown(e))
    }

/**
 * Variant for local storage operations — maps all non-cancellation exceptions to [DomainError.Storage].
 */
suspend inline fun <T> safeStorageCall(crossinline block: suspend () -> T): DomainResult<T> =
    try {
        DomainResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Timber.e(e, "DomainError: Storage")
        DomainResult.Failure(DomainError.Storage(e))
    }
