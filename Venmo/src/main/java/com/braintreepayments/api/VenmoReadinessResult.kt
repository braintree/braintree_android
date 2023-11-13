package com.braintreepayments.api

/**
 * Result of the [VenmoClient.isReadyToPay] check
 */
sealed class VenmoReadinessResult {

    /**
     * User is ready to pay with Venmo.
     */
    object ReadyToPay : VenmoReadinessResult()

    /**
     * User is not ready to pay with Venmo. Call [VenmoClient.showVenmoInGooglePlayStore] to
     * allow the user to install the Venmo app.
     */
    object NotReadyToPay : VenmoReadinessResult()

    /**
     * An [error] occurred when determining if the user is ready to pay with Venmo.
     */
    class Failure(val error: Exception) : VenmoReadinessResult()

}

