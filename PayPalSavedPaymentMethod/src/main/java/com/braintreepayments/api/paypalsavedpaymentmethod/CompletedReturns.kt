package com.braintreepayments.api.paypalsavedpaymentmethod

import androidx.lifecycle.SavedStateHandle
import java.security.MessageDigest

/**
 * The PayPal return that was completed last, kept in saved state so it survives process death and isn't completed
 * twice. Only a digest is kept, since a pending request is only compared for equality.
 */
internal class CompletedReturns(private val savedState: SavedStateHandle) {

    /** Records [pendingRequest] as completed. */
    fun mark(pendingRequest: String) {
        savedState[COMPLETED_PENDING_REQUEST_KEY] = pendingRequest.digest()
    }

    /** Whether [pendingRequest] was marked completed and not forgotten since. */
    fun contains(pendingRequest: String): Boolean =
        pendingRequest.digest() == savedState[COMPLETED_PENDING_REQUEST_KEY]

    /** Forgets the completed return, so the buyer can complete the same pending request later. */
    fun forget() {
        savedState.remove<String>(COMPLETED_PENDING_REQUEST_KEY)
    }

    private fun String.digest(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val COMPLETED_PENDING_REQUEST_KEY = "completed_pending_request"
    }
}
