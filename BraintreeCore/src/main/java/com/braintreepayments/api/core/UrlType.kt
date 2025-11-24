package com.braintreepayments.api.core

/**
 * The checkout uri that Gateway returns to us to be opened in either a browser or native PayPal app.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
internal annotation class CheckoutUri

/**
 * The return uri a merchant passes in to be used to redirect the user back to the merchant app after PayPal flow has
 * completed.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
internal annotation class MerchantReturnUri
