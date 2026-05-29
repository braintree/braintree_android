package com.braintreepayments.api.paypal

import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CompletableDeferred

internal class PendingPaymentStore {

    enum class State { IDLE, SWITCH_LAUNCHED, AWAITING_RETURN }

    data class PendingSession(
        val baToken: String,
        val clientMetadataId: String?,
        val merchantAccountId: String?,
        val intent: String?,
        val paymentType: String,
        val timestampMs: Long = System.currentTimeMillis(),
        val ttlMs: Long = TTL_MS
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestampMs > ttlMs
    }

    var state: State = State.IDLE
    var pendingSession: PendingSession? = null
    var tokenizeDeferred: CompletableDeferred<PayPalAccountNonce>? = null

    @Volatile
    var autoLinkNonce: PayPalAccountNonce? = null
    var originalPendingRequestString: String? = null

    var onReturnFromAppSwitch: (suspend () -> Unit)? = null

    @Synchronized
    fun getOrCreateDeferred(): Pair<CompletableDeferred<PayPalAccountNonce>, Boolean> {
        val existing = tokenizeDeferred
        if (existing != null) return Pair(existing, false)
        val new = CompletableDeferred<PayPalAccountNonce>()
        tokenizeDeferred = new
        return Pair(new, true)
    }

    fun clear() {
        state = State.IDLE
        pendingSession = null
        tokenizeDeferred?.cancel()
        tokenizeDeferred = null
        autoLinkNonce = null
        originalPendingRequestString = null
    }

    companion object {
        internal const val TTL_MS = 30 * 60 * 1000L

        val instance by lazy {
            val store = PendingPaymentStore()
            try {
                ProcessLifecycleOwner.get().lifecycle.addObserver(
                    AppForegroundDetector(store)
                )
            } catch (_: Exception) {
                // ProcessLifecycleOwner not available — Path 1 silently disabled.
                // Path 2 (re-click) still works independently.
            }
            store
        }
    }
}
