package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Exceptions thrown by the PayPal saved payment method module.
 */
@ExperimentalBetaApi
sealed class PayPalSavedPaymentMethodException(message: String) : BraintreeException(message) {

    /**
     * The authorization used to initialize the client is not a client token.
     */
    class InvalidAuthorization internal constructor() : PayPalSavedPaymentMethodException(
        "Invalid authorization. This feature can only be used with a client token."
    )

    /**
     * The client token does not carry a payment method ID JWT.
     */
    class MissingPaymentMethodIdJwt internal constructor() : PayPalSavedPaymentMethodException(
        "The client token is missing a payment method ID JWT. Generate it with a payment method ID."
    )

    /**
     * An order ID was not provided for a buyer updated billing agreement fetch.
     */
    class MissingOrderId internal constructor() : PayPalSavedPaymentMethodException(
        "An order ID is required to fetch the funding instrument selected on an approved checkout."
    )

    /**
     * An empty body was returned from the request and no error was returned.
     */
    class EmptyBodyReturned internal constructor() : PayPalSavedPaymentMethodException(
        "An empty body was returned from the request."
    )

    /**
     * The funding instrument details could not be parsed from the response.
     */
    class FailedToParseSummary internal constructor() : PayPalSavedPaymentMethodException(
        "Unable to parse the funding instrument details from the response."
    )

    /**
     * PayPal returned no Pay Later message to display for this buyer.
     */
    class MissingPreferredMessage internal constructor() : PayPalSavedPaymentMethodException(
        "No Pay Later message was returned for this buyer."
    )
}
