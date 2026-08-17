package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONArray
import org.json.JSONObject

/**
 * Used to request PayPal Pay Later / Credit presentment messaging from
 * `POST /v2/credit/fetch-presentment-messages`.
 *
 * Always requests Treatment A copy directly via [MessagePlacement.contentAttributes]; there is no
 * arm resolution at the network layer.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property flowContext Fixed BT-native flow context sent on every request.
 * @property messagePlacements The order amount(s) to fetch messaging for.
 */
@ExperimentalBetaApi
data class PayPalCreditMessagingRequest(
    val flowContext: FlowContext,
    val messagePlacements: List<MessagePlacement>
) {

    /**
     * @suppress
     */
    fun build(): JSONObject = JSONObject()
        .put(FLOW_CONTEXT_KEY, flowContext.toJson())
        .put(MESSAGE_PLACEMENTS_KEY, JSONArray(messagePlacements.map { it.toJson() }))

    private companion object {
        const val FLOW_CONTEXT_KEY = "flow_context"
        const val MESSAGE_PLACEMENTS_KEY = "message_placements"
    }
}

/**
 * Fixed BT-native flow context sent on every credit presentment messaging request.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property attributes Flow attributes identifying the brand and platform.
 * @property channel Fixed to "MOBILE_APP".
 * @property flowSpecifier Fixed to "EARLY_PRESENTMENT".
 */
@ExperimentalBetaApi
data class FlowContext(
    val attributes: List<String> = listOf("BRAND_BRAINTREE", "EXPERIENCE_ANDROID_SDK"),
    val channel: String = "MOBILE_APP",
    val flowSpecifier: String = "EARLY_PRESENTMENT"
) {

    internal fun toJson(): JSONObject = JSONObject()
        .put(ATTRIBUTES_KEY, JSONArray(attributes))
        .put(CHANNEL_KEY, channel)
        .put(FLOW_SPECIFIER_KEY, flowSpecifier)

    private companion object {
        const val ATTRIBUTES_KEY = "attributes"
        const val CHANNEL_KEY = "channel"
        const val FLOW_SPECIFIER_KEY = "flow_specifier"
    }
}

/**
 * A single order amount to fetch presentment messaging for.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property amount The order amount driving the messaging copy.
 * @property contentAttributes Selects the Treatment A ("Or" copy, compact) message variant.
 */
@ExperimentalBetaApi
data class MessagePlacement(
    val amount: Amount,
    val contentAttributes: List<String> = listOf(
        "ALTERNATIVE_PREFIX_UPPERCASE_OR",
        "MESSAGE_LENGTH_COMPACT"
    )
) {

    internal fun toJson(): JSONObject = JSONObject()
        .put(AMOUNT_KEY, amount.toJson())
        .put(CONTENT_ATTRIBUTES_KEY, JSONArray(contentAttributes))

    private companion object {
        const val AMOUNT_KEY = "amount"
        const val CONTENT_ATTRIBUTES_KEY = "content_attributes"
    }
}

/**
 * An order amount.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property currencyCode ISO currency code, e.g. "USD".
 * @property value The amount, e.g. "55.00".
 */
@ExperimentalBetaApi
data class Amount(
    val currencyCode: String,
    val value: String
) {

    internal fun toJson(): JSONObject = JSONObject()
        .put(CURRENCY_CODE_KEY, currencyCode)
        .put(VALUE_KEY, value)

    private companion object {
        const val CURRENCY_CODE_KEY = "currency_code"
        const val VALUE_KEY = "value"
    }
}
