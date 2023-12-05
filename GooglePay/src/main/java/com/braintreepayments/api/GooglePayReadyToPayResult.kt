package com.braintreepayments.api

/**
 * Result of [GooglePayClient.isReadyToPay]
 */
sealed class GooglePayReadyToPayResult {

    /**
     * The Google Pay API is supported and set up on this device. Show the Google Pay button for
     * Google Pay.
     */
    object ReadyToPay : GooglePayReadyToPayResult()

    /**
     * The Google Pay API is supported or not set up on this device.
     */
    object NotReadyToPay : GooglePayReadyToPayResult()

    /**
     * There was an [error] determining readiness to Google Pay
     */
    class Failure(val error: Exception) : GooglePayReadyToPayResult()

}
