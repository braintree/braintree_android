package com.braintreepayments.api.paypal

import android.content.Context
import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences
import java.util.concurrent.TimeUnit

/**
 * Persists the pending request string returned by [PayPalLauncher.launch] so the PayPal flow can
 * be completed even if the merchant app process is terminated while the user is in the PayPal app
 * or browser (for example, when Android reclaims memory on low-RAM devices during App Switch).
 */
internal class PayPalPendingRequestStore(
    private val sharedPreferences: BraintreeSharedPreferences,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() }
) {

    fun save(pendingRequestString: String) {
        sharedPreferences.putStringAndLong(
            PENDING_REQUEST_KEY,
            pendingRequestString,
            TIMESTAMP_KEY,
            currentTimeMillis()
        )
    }

    /**
     * @return the stored pending request string, or null if none is stored or it has expired.
     */
    fun get(): String? {
        val pendingRequestString = sharedPreferences.getString(PENDING_REQUEST_KEY, null)
        if (pendingRequestString.isNullOrEmpty()) {
            return null
        }

        val age = currentTimeMillis() - sharedPreferences.getLong(TIMESTAMP_KEY)
        if (age < 0 || age > TIME_TO_LIVE) {
            clear()
            return null
        }
        return pendingRequestString
    }

    fun clear() {
        sharedPreferences.remove(PENDING_REQUEST_KEY, TIMESTAMP_KEY)
    }

    companion object {
        internal const val PENDING_REQUEST_KEY = "com.braintreepayments.api.paypal.PENDING_REQUEST"
        internal const val TIMESTAMP_KEY = "com.braintreepayments.api.paypal.PENDING_REQUEST_TIMESTAMP"

        // A PayPal checkout session does not outlive its EC token; discard anything older.
        internal val TIME_TO_LIVE = TimeUnit.HOURS.toMillis(3)

        @Volatile
        private var INSTANCE: PayPalPendingRequestStore? = null

        fun getInstance(context: Context): PayPalPendingRequestStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PayPalPendingRequestStore(
                    BraintreeSharedPreferences.getInstance(context.applicationContext)
                ).also { INSTANCE = it }
            }
    }
}
