package com.braintreepayments.api.paypalsavedpaymentmethod

import org.json.JSONArray
import org.json.JSONObject

/**
 * The POST body for `v2/credit/fetch-presentment-messages`. The flow context and content attributes are fixed.
 *
 * @property amount The order amount the message is calculated from, for example `"55.00"`.
 * @property currencyCode The ISO-4217 currency code for [amount], for example `"USD"`.
 */
internal data class PayPalCreditMessagingRequest(
    val amount: String,
    val currencyCode: String
) {

    fun toJson(): JSONObject = JSONObject()
        .put(FLOW_CONTEXT_KEY, FlowContext().toJson())
        .put(
            MESSAGE_PLACEMENTS_KEY,
            JSONArray().put(MessagePlacement(Amount(currencyCode, amount)).toJson())
        )

    private companion object {
        const val FLOW_CONTEXT_KEY = "flow_context"
        const val MESSAGE_PLACEMENTS_KEY = "message_placements"
    }
}

private data class FlowContext(
    val attributes: List<String> = listOf(
        "BRAND_BRAINTREE",
        "EXPERIENCE_ANDROID_SDK",
        "EXPERIENCE_VIEW_EDIT_FI",
        "EXPERIENCE_EXTERNAL_DIRECT"
    ),
    val channel: String = "MOBILE_APP",
    val flowSpecifier: String = "EARLY_PRESENTMENT"
) {

    fun toJson(): JSONObject = JSONObject()
        .put(ATTRIBUTES_KEY, JSONArray(attributes))
        .put(CHANNEL_KEY, channel)
        .put(FLOW_SPECIFIER_KEY, flowSpecifier)

    private companion object {
        const val ATTRIBUTES_KEY = "attributes"
        const val CHANNEL_KEY = "channel"
        const val FLOW_SPECIFIER_KEY = "flow_specifier"
    }
}

private data class MessagePlacement(
    val amount: Amount,
    val contentAttributes: List<String> = listOf("ALTERNATIVE_PREFIX_UPPERCASE_OR", "MESSAGE_LENGTH_COMPACT")
) {

    fun toJson(): JSONObject = JSONObject()
        .put(AMOUNT_KEY, amount.toJson())
        .put(CONTENT_ATTRIBUTES_KEY, JSONArray(contentAttributes))

    private companion object {
        const val AMOUNT_KEY = "amount"
        const val CONTENT_ATTRIBUTES_KEY = "content_attributes"
    }
}

private data class Amount(
    val currencyCode: String,
    val value: String
) {

    fun toJson(): JSONObject = JSONObject()
        .put(CURRENCY_CODE_KEY, currencyCode)
        .put(VALUE_KEY, value)

    private companion object {
        const val CURRENCY_CODE_KEY = "currency_code"
        const val VALUE_KEY = "value"
    }
}
