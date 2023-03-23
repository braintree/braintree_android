package com.braintreepayments.api

import android.content.Context
import androidx.annotation.VisibleForTesting

//NEXT MAJOR VERSION: remove open modifier
/**
 * Fetches information about which payment methods are preferred on the device.
 * Used to determine which payment methods are given preference in your UI,
 * not whether they are presented entirely.
 * This class is currently in beta and may be removed in future releases.
 * @hide
 */
open class PreferredPaymentMethodsClient @VisibleForTesting internal constructor(
    private val braintreeClient: BraintreeClient, private val deviceInspector: DeviceInspector
) {
    constructor(braintreeClient: BraintreeClient) : this(braintreeClient, DeviceInspector()) {}

    /**
     * Fetches information about which payment methods should be given preference in your UI.
     *
     * @param context Android Context
     * @param callback [PreferredPaymentMethodsCallback]
     */
    open fun fetchPreferredPaymentMethods(context: Context, callback: PreferredPaymentMethodsCallback) {
        val applicationContext = context.applicationContext
        val isVenmoAppInstalled = deviceInspector.isVenmoInstalled(applicationContext)
        val isPayPalAppInstalled = deviceInspector.isPayPalInstalled(applicationContext)

        val venmoAppInstalledEvent = "preferred-payment-methods.venmo.app-installed.$isVenmoAppInstalled"
        braintreeClient.sendAnalyticsEvent(venmoAppInstalledEvent)

        if (isPayPalAppInstalled) {
            braintreeClient.sendAnalyticsEvent("preferred-payment-methods.paypal.app-installed.true")
            callback.onResult(
                PreferredPaymentMethodsResult()
                    .isPayPalPreferred(true)
                    .isVenmoPreferred(isVenmoAppInstalled)
            )
            return
        }
        braintreeClient.getConfiguration(ConfigurationCallback { configuration, _ ->
            val isGraphQLDisabled = configuration == null || !configuration.isGraphQLEnabled
            if (isGraphQLDisabled) {
                braintreeClient.sendAnalyticsEvent("preferred-payment-methods.api-disabled")
                callback.onResult(
                    PreferredPaymentMethodsResult()
                        .isPayPalPreferred(isPayPalAppInstalled)
                        .isVenmoPreferred(isVenmoAppInstalled)
                )
                return@ConfigurationCallback
            }
            val query =
                "{ \"query\": \"query PreferredPaymentMethods { preferredPaymentMethods { paypalPreferred } }\" }"
            braintreeClient.sendGraphQLPOST(query, object : HttpResponseCallback {
                override fun onResult(responseBody: String?, httpError: Exception?) {
                    if (responseBody != null) {
                        val result = PreferredPaymentMethodsResult.fromJSON(
                            responseBody,
                            isVenmoAppInstalled
                        )
                        val payPalPreferredEvent =
                            "preferred-payment-methods.paypal.api-detected.${result.isPayPalPreferred()}"
                        braintreeClient.sendAnalyticsEvent(payPalPreferredEvent)
                        callback.onResult(result)
                    } else {
                        braintreeClient.sendAnalyticsEvent("preferred-payment-methods.api-error")
                        callback.onResult(
                            PreferredPaymentMethodsResult()
                                .isPayPalPreferred(false)
                                .isVenmoPreferred(isVenmoAppInstalled)
                        )
                    }
                }
            })
        })
    }
}