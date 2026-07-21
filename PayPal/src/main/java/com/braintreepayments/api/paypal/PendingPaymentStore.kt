package com.braintreepayments.api.paypal

import kotlinx.coroutines.CompletableDeferred

/**
 * Process-level singleton that holds state for the auto-link on manual return flow.
 *
 * When a PayPal app switch completes but the App Link return fails, the user manually
 * switches back to the merchant app. This store persists the billing agreement token and
 * session metadata across that round trip so the SDK can tokenize directly with BTGW
 * without a URL return.
 *
 * A [CompletableDeferred] ensures exactly one BTGW tokenization call is made, even when
 * multiple entry points (handle-return NoResult and user re-click) race to tokenize.
 *
 * This class must outlive individual [PayPalClient] instances because the client is
 * per-Fragment and may be GC'd during the app switch.
 */
internal class PendingPaymentStore {

    /**
     * Captured session data needed to tokenize a billing agreement without a URL return.
     *
     * @property baToken the billing agreement token from the PayPal approval URL
     * @property clientMetadataId correlation ID for fraud detection
     * @property merchantAccountId optional merchant account override
     * @property intent the PayPal payment intent string value (e.g. "authorize", "sale")
     * @property paymentType always "billing-agreement" for auto-link flows
     * @property timestampMs creation time for TTL expiry checks
     * @property ttlMs time-to-live in milliseconds (default 30 minutes)
     */
    data class PendingSession(
        val baToken: String,
        val clientMetadataId: String?,
        val merchantAccountId: String?,
        val intent: String?,
        val paymentType: String,
        val timestampMs: Long = System.currentTimeMillis(),
        val ttlMs: Long = TTL_MS
    ) {
        /** Returns true if this session has exceeded its time-to-live. */
        fun isExpired(): Boolean = System.currentTimeMillis() - timestampMs > ttlMs
    }

    @Volatile
    var pendingSession: PendingSession? = null

    @Volatile
    var tokenizeDeferred: CompletableDeferred<PayPalAccountNonce>? = null

    /** Resolved nonce from auto-link tokenization. Volatile for safe cross-thread reads. */
    @Volatile
    var autoLinkNonce: PayPalAccountNonce? = null

    /**
     * Returns an existing or newly created [CompletableDeferred] along with a flag
     * indicating whether this caller is the initiator (created the deferred).
     *
     * The initiator is responsible for making the BTGW call and completing the deferred.
     * Subsequent callers receive the same deferred and should await it.
     *
     * @return pair of (deferred, isInitiator)
     */
    @Synchronized
    fun getOrCreateDeferred(): Pair<CompletableDeferred<PayPalAccountNonce>, Boolean> {
        val existing = tokenizeDeferred
        if (existing != null) return Pair(existing, false)
        val new = CompletableDeferred<PayPalAccountNonce>()
        tokenizeDeferred = new
        return Pair(new, true)
    }

    /** Resets all state and cancels any in-flight deferred. */
    fun clear() {
        pendingSession = null
        tokenizeDeferred?.cancel()
        tokenizeDeferred = null
        autoLinkNonce = null
    }

    companion object {
        /** Default session TTL: 30 minutes. */
        internal const val TTL_MS = 30 * 60 * 1000L

        val instance by lazy { PendingPaymentStore() }
    }
}
