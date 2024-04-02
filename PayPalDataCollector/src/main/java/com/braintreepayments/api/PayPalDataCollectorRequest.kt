package com.braintreepayments.api

/**
 * Parameters needed when the SDK collects data for fraud identification purposes
 *
 * @property hasUserLocationConsent is an optional parameter that informs the SDK
 * if your application has obtained consent from the user to collect location data in compliance with
 * <a href="https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive">Google Play Developer Program policies</a>
 * This flag enables PayPal to collect necessary information required for Fraud Detection and Risk Management.
 *
 * @see <a href="https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive">User Data policies for the Google Play Developer Program </a>
 * @see <a href="https://support.google.com/googleplay/android-developer/answer/9799150?hl=en#Prominent%20in-app%20disclosure">Examples of prominent in-app disclosures</a>
 */
data class PayPalDataCollectorRequest(
    val hasUserLocationConsent: Boolean
)
