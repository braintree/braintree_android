package com.braintreepayments.api

import org.json.JSONObject
import java.util.ArrayList

/**
 * Contains the remote Visa Checkout configuration for the Braintree SDK.
 * @property isEnabled Determines if the Visa Checkout flow is available to be used. This can be used to determine
 *                      if UI components should be shown or hidden.
 * @property apiKey The Visa Checkout API Key associated with this merchant's Visa Checkout configuration.
 * @property externalClientId The Visa Checkout API Key associated with this merchant's Visa Checkout configuration.
 * @property acceptedCardBrands The accepted card brands for Visa Checkout.
 */
internal data class VisaCheckoutConfiguration(
    val apiKey: String,
    val externalClientId: String,
    val acceptedCardBrands: List<String>
) {

    val isEnabled: Boolean = apiKey != ""

    constructor(json: JSONObject?) : this(
        Json.optString(json, API_KEY, ""),
        Json.optString(json, EXTERNAL_CLIENT_ID, ""),
        supportedCardTypesToAcceptedCardBrands(
            CardConfiguration(json).supportedCardTypes
        )
    )

    companion object {

        private const val API_KEY = "apikey"
        private const val EXTERNAL_CLIENT_ID = "externalClientId"

        private fun supportedCardTypesToAcceptedCardBrands(supportedCardTypes: List<String>): List<String> {
            val acceptedCardBrands: MutableList<String> = ArrayList()
            for (supportedCardType in supportedCardTypes) {
                when (supportedCardType.lowercase()) {
                    "visa" -> acceptedCardBrands.add("VISA")
                    "mastercard" -> acceptedCardBrands.add("MASTERCARD")
                    "discover" -> acceptedCardBrands.add("DISCOVER")
                    "american express" -> acceptedCardBrands.add("AMEX")
                }
            }
            return acceptedCardBrands
        }
    }
}
