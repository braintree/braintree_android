package com.braintreepayments.api

/**
 * Callback for receiving result of [VenmoClient.isReadyToPay].
 */
fun interface VenmoIsReadyToPayCallback {

    /**
     * @param venmoReadinessResult true if Venmo is ready; false otherwise.
     */
    fun onVenmoReadinessResult(venmoReadinessResult: VenmoReadinessResult)
}
