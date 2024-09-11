package com.braintreepayments.api.googlepay

/**
 * Result of [GooglePayClient.isReadyToPay]
 */
sealed class GooglePayReadinessResult {

    /**
     * The Google Pay API is supported and set up on this device. Show the Google Pay button for
     * Google Pay.
     */
    data object ReadyToPay : GooglePayReadinessResult()

    /**
     * The Google Pay API is supported or not set up on this device, or there was an issue [error]
     * determining readiness.
     */
    class NotReadyToPay(val error: Exception?) : GooglePayReadinessResult()
}
