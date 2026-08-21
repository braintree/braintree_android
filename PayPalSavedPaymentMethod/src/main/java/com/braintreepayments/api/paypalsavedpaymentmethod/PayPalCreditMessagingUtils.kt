package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds [PayPalCreditMessagingContent] from a `preferred_message.content` JSON object. Shared by both
 * the classic View and Compose UI so the message-concatenation and learn-more-selection rules live
 * in one place instead of being duplicated per UI surface.
 */
@ExperimentalBetaApi
internal object PayPalCreditMessagingUtils {

    private const val MAIN_ITEMS_KEY = "main_items"
    private const val DISCLAIMER_ITEMS_KEY = "disclaimer_items"
    private const val ACTION_ITEMS_KEY = "action_items"
    private const val TYPE_KEY = "type"
    private const val TEXT_KEY = "text"
    private const val ALTERNATIVE_TEXT_KEY = "alternative_text"
    private const val CLICK_URL_KEY = "click_url"
    private const val IMAGE_TYPE = "IMAGE"
    private const val LINK_TYPE = "LINK"

    fun buildContent(content: JSONObject): PayPalCreditMessagingContent {
        val learnMoreAction = learnMoreAction(content)
        return PayPalCreditMessagingContent(
            message = buildMessage(content),
            learnMoreText = learnMoreAction?.optString(TEXT_KEY).orEmpty(),
            learnMoreUrl = learnMoreAction?.optString(CLICK_URL_KEY).orEmpty()
        )
    }

    // main_items already carry their own spacing (e.g. "...mit " + logo + "."), so they're joined
    // with no separator. disclaimer_items is a separate, optional sentence (e.g. "Nur mit dt.
    // PayPal Konto."), so when present it's appended after a space.
    private fun buildMessage(content: JSONObject): String {
        val main = itemsText(content.optJSONArray(MAIN_ITEMS_KEY))
        val disclaimer = itemsText(content.optJSONArray(DISCLAIMER_ITEMS_KEY))
        return if (disclaimer.isEmpty()) main else "$main $disclaimer"
    }

    // IMAGE blocks (e.g. the PayPal logo) render as their alternative_text instead of being
    // dropped, so the message still reads naturally without a second inline logo - the FI row
    // already shows one. This substitution only applies to message content (main/disclaimer
    // items); the learn-more action's own text is read separately in learnMoreAction.
    private fun itemsText(items: JSONArray?): String {
        if (items == null) return ""
        return (0 until items.length())
            .map { items.getJSONObject(it) }
            .joinToString("") {
                if (it.optString(TYPE_KEY) == IMAGE_TYPE) {
                    it.optString(ALTERNATIVE_TEXT_KEY)
                } else {
                    it.optString(TEXT_KEY)
                }
            }
    }

    private fun learnMoreAction(content: JSONObject): JSONObject? {
        val actionItems = content.optJSONArray(ACTION_ITEMS_KEY) ?: return null
        return (0 until actionItems.length())
            .map { actionItems.getJSONObject(it) }
            .firstOrNull { it.optString(TYPE_KEY) == LINK_TYPE }
    }
}
