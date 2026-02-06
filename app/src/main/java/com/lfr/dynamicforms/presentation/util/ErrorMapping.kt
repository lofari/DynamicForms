package com.lfr.dynamicforms.presentation.util

import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Exception.toUserMessage(): String = when (this) {
    is UnknownHostException -> "No internet connection. Check your network and try again."
    is SocketTimeoutException -> "Request timed out. Please try again."
    is retrofit2.HttpException -> when (code()) {
        404 -> "Form not found."
        in 500..599 -> "Server error. Please try again later."
        else -> "Request failed. Please try again."
    }
    else -> "Something went wrong. Please try again."
}
