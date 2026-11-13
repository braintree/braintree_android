package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.paypal.PayPalResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

/** Results no view has handled yet, oldest first. */
internal class PendingResults {

    private val pending = MutableStateFlow<List<PayPalResult>>(emptyList())

    /**
     * Delivers each result at least once: a result whose collector is cancelled goes to the next collector. A channel
     * would lose it, which is why this isn't one.
     */
    val results: Flow<PayPalResult> = flow {
        pending.collect { results ->
            val result = results.firstOrNull() ?: return@collect
            try {
                emit(result)
            } finally {
                // Still active means the collector handled it.
                if (currentCoroutineContext().isActive) {
                    pending.update { if (it.firstOrNull() === result) it.drop(1) else it }
                }
            }
        }
    }

    /** Queues [result] behind the ones not handled yet. */
    fun add(result: PayPalResult) {
        pending.update { it + result }
    }
}
