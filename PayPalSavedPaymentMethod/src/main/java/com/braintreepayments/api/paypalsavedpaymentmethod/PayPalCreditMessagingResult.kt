package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONObject

/**
 * A single copy or logo block making up a presentment message, from
 * `preferred_message.content.main_items`.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
@ExperimentalBetaApi
sealed class MessageItem {

    /**
     * A text block.
     *
     * @property text Copy for the block.
     * @property name Variable name for the block, e.g. "periodic_payment_count".
     */
    data class Text(val text: String, val name: String? = null) : MessageItem()

    /**
     * An image block, e.g. the PayPal logo. Callers should skip rendering these - the FI row
     * already shows the payment logo, so the message row is copy-only.
     *
     * @property sourceUrl Image URL.
     * @property alternativeText Accessibility text for the image.
     * @property name Variable name for the block, e.g. "paypal_logo".
     */
    data class Image(
        val sourceUrl: String,
        val alternativeText: String? = null,
        val name: String? = null
    ) : MessageItem()

    internal companion object {

        private const val TYPE_KEY = "type"
        private const val TEXT_KEY = "text"
        private const val NAME_KEY = "name"
        private const val SOURCE_URL_KEY = "source_url"
        private const val ALTERNATIVE_TEXT_KEY = "alternative_text"
        private const val IMAGE_TYPE = "IMAGE"

        fun fromJson(json: JSONObject): MessageItem {
            val name = json.optString(NAME_KEY).takeIf { json.has(NAME_KEY) }
            return if (json.optString(TYPE_KEY) == IMAGE_TYPE) {
                Image(
                    sourceUrl = json.optString(SOURCE_URL_KEY),
                    alternativeText = json.optString(ALTERNATIVE_TEXT_KEY)
                        .takeIf { json.has(ALTERNATIVE_TEXT_KEY) },
                    name = name
                )
            } else {
                Text(text = json.optString(TEXT_KEY), name = name)
            }
        }
    }
}

/**
 * The parsed result of a successful `/v2/credit/fetch-presentment-messages` fetch, flattened from
 * `messages[0].preferred_message`. A null result (fetch failed or no `preferred_message`) means the
 * messaging row should be hidden - the FI card still renders.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property messageId The message id.
 * @property messageType The message template id, e.g. "PLLT_MQ_GZ".
 * @property messageItems Copy + logo blocks to render inline, from `content.main_items`.
 * @property learnMoreText Display text for the "Learn more" action, if present.
 * @property learnMoreUrl URL to open when "Learn more" is tapped, if present.
 * @property impressionUrl Analytics URL to fire when the message is displayed.
 */
@ExperimentalBetaApi
data class PayPalCreditMessagingResult(
    val messageId: String,
    val messageType: String,
    val messageItems: List<MessageItem>,
    val learnMoreText: String?,
    val learnMoreUrl: String?,
    val impressionUrl: String?
) {

    internal companion object {

        private const val PREFERRED_MESSAGE_KEY = "preferred_message"
        private const val ID_KEY = "id"
        private const val TYPE_KEY = "type"
        private const val CONTENT_KEY = "content"
        private const val MAIN_ITEMS_KEY = "main_items"
        private const val ACTION_ITEMS_KEY = "action_items"
        private const val ACTION_TYPE_KEY = "type"
        private const val LINK_TYPE = "LINK"
        private const val TEXT_KEY = "text"
        private const val CLICK_URL_KEY = "click_url"
        private const val ANALYTICS_KEY = "analytics"
        private const val IMPRESSION_URL_KEY = "impression_url"

        fun fromJson(messageJson: JSONObject): PayPalCreditMessagingResult {
            val preferredMessage = messageJson.optJSONObject(PREFERRED_MESSAGE_KEY) ?: JSONObject()
            val content = preferredMessage.optJSONObject(CONTENT_KEY)

            val messageItems = content?.optJSONArray(MAIN_ITEMS_KEY)?.let { items ->
                (0 until items.length()).map { MessageItem.fromJson(items.getJSONObject(it)) }
            }.orEmpty()

            val learnMoreAction = content?.optJSONArray(ACTION_ITEMS_KEY)?.let { items ->
                (0 until items.length())
                    .map { items.getJSONObject(it) }
                    .firstOrNull { it.optString(ACTION_TYPE_KEY) == LINK_TYPE }
            }

            return PayPalCreditMessagingResult(
                messageId = preferredMessage.optString(ID_KEY),
                messageType = preferredMessage.optString(TYPE_KEY),
                messageItems = messageItems,
                learnMoreText = learnMoreAction?.optString(TEXT_KEY)?.takeIf { it.isNotEmpty() },
                learnMoreUrl = learnMoreAction?.optString(CLICK_URL_KEY)?.takeIf { it.isNotEmpty() },
                impressionUrl = preferredMessage.optJSONObject(ANALYTICS_KEY)
                    ?.optString(IMPRESSION_URL_KEY)
                    ?.takeIf { it.isNotEmpty() }
            )
        }
    }
}
