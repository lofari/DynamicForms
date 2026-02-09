package com.lfr.dynamicforms.presentation.util

import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val HTTP_NOT_FOUND = 404
private const val HTTP_SERVER_ERROR_MIN = 500
private const val HTTP_SERVER_ERROR_MAX = 599

fun Throwable.toUserMessage(): String = when (this) {
    is UnknownHostException -> "No internet connection. Check your network and try again."
    is SocketTimeoutException -> "Request timed out. Please try again."
    is retrofit2.HttpException -> when (code()) {
        HTTP_NOT_FOUND -> "Form not found."
        in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX -> "Server error. Please try again later."
        else -> "Request failed. Please try again."
    }
    else -> "Something went wrong. Please try again."
}
