package com.braintreepayments.api

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun CardClient.tokenize(card: Card) =
    suspendCancellableCoroutine<CardNonce> { continuation ->
        tokenize(card) { cardNonce, error ->
            cardNonce?.let {
                continuation.resume(it)
            }
            error?.let {
                continuation.resumeWithException(it)
            }
        }
    }