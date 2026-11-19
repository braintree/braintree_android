package com.braintreepayments.api.paymentactions

import androidx.fragment.app.FragmentActivity

/**
 * Seam between [ChallengeShellActivity] and the provider's challenge UI. Same shape as
 * `registerForActivityResult`: register in `onCreate`, then start.
 */
internal fun interface ChallengeRegistrar {

    /** Call in every `onCreate`, including recreations. [onResult] may run on any thread, more than once. */
    fun register(activity: FragmentActivity, onResult: (ChallengeResult) -> Unit): ChallengeStarter
}

/** Starts a challenge registered through [ChallengeRegistrar.register]. */
internal fun interface ChallengeStarter {

    /**
     * Opens the challenge screen. Call once: Cardinal fails a challenge that's started twice.
     *
     * @throws RuntimeException if the provider rejects the request.
     */
    fun start(transactionId: String, payload: String)
}
