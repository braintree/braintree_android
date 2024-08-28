package com.braintreepayments.api.googlepay

import android.content.Context
import com.braintreepayments.api.core.Configuration
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.wallet.Wallet

/**
 * Class representing Google Pay payment capabilities
 */
object GooglePayCapabilities {
    /**
     * @return `true` if Google Pay is enabled and supported in the current environment,
     * `false` otherwise. Note: this value only pertains to the Braintree configuration, to check if
     * the user has Google Pay setup use [GooglePayClient.isReadyToPay]
     */
    @SuppressWarnings("SwallowedException")
    fun isGooglePayEnabled(context: Context, configuration: Configuration): Boolean {
        return try {
            Class.forName(Wallet::class.java.name)

            configuration.isGooglePayEnabled && GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) ==
                    ConnectionResult.SUCCESS
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: NoClassDefFoundError) {
            false
        }
    }
}
