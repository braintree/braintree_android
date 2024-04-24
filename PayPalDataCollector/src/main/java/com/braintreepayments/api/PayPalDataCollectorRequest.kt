package com.braintreepayments.api

/**
 * Parameters needed when the SDK collects data for fraud identification purposes
 *
 * @property hasUserLocationConsent is an optional parameter that informs the SDK
 * if your application has obtained consent from the user to collect location data in compliance with
 * [Google Play Developer Program policies](https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive)
 * This flag enables PayPal to collect necessary information required for Fraud Detection and Risk Management.
 *
 * @see [User Data policies for the Google Play Developer Program](https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive)
 * @see [Examples of prominent in-app disclosures](https://support.google.com/googleplay/android-developer/answer/9799150?hl=en#Prominent%20in-app%20disclosure)
 *
 * @property riskCorrelationId Optional client metadata id
 */
data class PayPalDataCollectorRequest(
    val hasUserLocationConsent: Boolean,
    val riskCorrelationId: String? = null
)
