package com.braintreepayments.api.paypalsavedpaymentmethod.model

import com.braintreepayments.api.paypalsavedpaymentmethod.asHttpsUrlOrNull
import com.braintreepayments.api.sharedutils.Json
import org.json.JSONObject

/**
 * A single block of a presentment message. Blocks are returned in the order they must be displayed.
 *
 * @property type The kind of block, or `null` when PayPal returns a type this SDK version does not recognize.
 * @property text The text to display, for example `"4 interest-free payments of $13.75 with "`.
 * @property alternativeText The screen reader text for blocks whose [text] relies on symbols or abbreviations.
 * @property clickUrl The URL to open when a link or image block is tapped. `null` unless PayPal returns an https URL.
 * @property sourceUrl The image to render for an image block. `null` unless PayPal returns an https URL.
 * @property name A non-unique identifier for the block, for example `"periodic_payment_count"` or `"paypal_logo"`.
 * @property isEmbeddable Whether [clickUrl] may be loaded in an embedded web view rather than an external browser.
 */
internal data class PayPalCreditMessageItem(
    val type: PayPalCreditMessageItemType?,
    val text: String?,
    val alternativeText: String?,
    val clickUrl: String?,
    val sourceUrl: String?,
    val name: String?,
    val isEmbeddable: Boolean?
) {

    companion object {
        private const val TYPE_KEY = "type"
        private const val TEXT_KEY = "text"
        private const val ALTERNATIVE_TEXT_KEY = "alternative_text"
        private const val CLICK_URL_KEY = "click_url"
        private const val SOURCE_URL_KEY = "source_url"
        private const val NAME_KEY = "name"
        private const val EMBEDDABLE_KEY = "embeddable"

        /**
         * Parses a single content block of a presentment message. Returns `null` when the block is not an object.
         */
        fun fromJson(json: JSONObject?): PayPalCreditMessageItem? {
            if (json == null) return null
            return PayPalCreditMessageItem(
                type = PayPalCreditMessageItemType.fromRawValue(Json.optString(json, TYPE_KEY, null)),
                text = Json.optString(json, TEXT_KEY, null),
                alternativeText = Json.optString(json, ALTERNATIVE_TEXT_KEY, null),
                clickUrl = Json.optString(json, CLICK_URL_KEY, null).asHttpsUrlOrNull(),
                sourceUrl = Json.optString(json, SOURCE_URL_KEY, null).asHttpsUrlOrNull(),
                name = Json.optString(json, NAME_KEY, null),
                isEmbeddable = if (json.isNull(EMBEDDABLE_KEY)) null else json.optBoolean(EMBEDDABLE_KEY)
            )
        }
    }
}
