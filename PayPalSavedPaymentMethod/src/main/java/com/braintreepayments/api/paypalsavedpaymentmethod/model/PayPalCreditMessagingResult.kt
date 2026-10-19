package com.braintreepayments.api.paypalsavedpaymentmethod.model

import com.braintreepayments.api.paypalsavedpaymentmethod.asHttpsUrlOrNull
import com.braintreepayments.api.sharedutils.Json
import org.json.JSONArray
import org.json.JSONObject

/**
 * The Pay Later message to display alongside a vaulted PayPal payment method.
 *
 * @property mainItems The main text and logo blocks of the message.
 * @property disclaimerItems Legal disclaimers that PayPal requires to be displayed with [mainItems].
 * @property actionItems The interactive blocks of the message, such as the "Learn more" link.
 * @property messageId The identifier of the message that was selected.
 * @property messageType The template the message was built from, for example `"PLST_SQ"`.
 * @property impressionUrl The tracking beacon to fire once the message is on screen. `null` unless PayPal returns an
 * https URL.
 */
internal data class PayPalCreditMessagingResult(
    val mainItems: List<PayPalCreditMessageItem>,
    val disclaimerItems: List<PayPalCreditMessageItem>,
    val actionItems: List<PayPalCreditMessageItem>,
    val messageId: String?,
    val messageType: String?,
    val impressionUrl: String?
) {

    companion object {
        private const val MESSAGES_KEY = "messages"
        private const val PREFERRED_MESSAGE_KEY = "preferred_message"
        private const val CONTENT_KEY = "content"
        private const val MAIN_ITEMS_KEY = "main_items"
        private const val DISCLAIMER_ITEMS_KEY = "disclaimer_items"
        private const val ACTION_ITEMS_KEY = "action_items"
        private const val ID_KEY = "id"
        private const val TYPE_KEY = "type"
        private const val ANALYTICS_KEY = "analytics"
        private const val IMPRESSION_URL_KEY = "impression_url"

        /**
         * Parses the preferred message out of a `v2/credit/fetch-presentment-messages` response. Returns `null` when
         * PayPal has no message to show for this buyer.
         */
        fun fromJson(json: JSONObject): PayPalCreditMessagingResult? {
            val preferredMessage = json
                .optJSONArray(MESSAGES_KEY)
                ?.optJSONObject(0)
                ?.optJSONObject(PREFERRED_MESSAGE_KEY)
                ?: return null
            val content = preferredMessage.optJSONObject(CONTENT_KEY) ?: return null
            val mainItems = content.optJSONArray(MAIN_ITEMS_KEY).toItems()

            // Reporting success with no copy would fire the impression beacon for a message the buyer never saw.
            // Image blocks carry their copy in alternativeText rather than text.
            if (mainItems.all { it.text.isNullOrEmpty() && it.alternativeText.isNullOrEmpty() }) return null

            return PayPalCreditMessagingResult(
                mainItems = mainItems,
                disclaimerItems = content.optJSONArray(DISCLAIMER_ITEMS_KEY).toItems(),
                actionItems = content.optJSONArray(ACTION_ITEMS_KEY).toItems(),
                messageId = Json.optString(preferredMessage, ID_KEY, null),
                messageType = Json.optString(preferredMessage, TYPE_KEY, null),
                impressionUrl = Json.optString(preferredMessage.optJSONObject(ANALYTICS_KEY), IMPRESSION_URL_KEY, null)
                    .asHttpsUrlOrNull()
            )
        }

        private fun JSONArray?.toItems(): List<PayPalCreditMessageItem> {
            if (this == null) return emptyList()
            return (0 until length()).mapNotNull { PayPalCreditMessageItem.fromJson(optJSONObject(it)) }
        }
    }
}
