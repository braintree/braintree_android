package com.braintreepayments.api

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun CardClient.tokenize(context: Context, card: Card) =
    suspendCancellableCoroutine<CardNonce> { continuation ->
        tokenize(context, card) { cardNonce, error ->
            cardNonce?.let {
                continuation.resume(it)
            }
            error?.let {
                continuation.resumeWithException(it)
            }
        }
    }