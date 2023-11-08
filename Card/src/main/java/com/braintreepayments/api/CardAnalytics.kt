package com.braintreepayments.api

internal enum class CardAnalytics(@JvmField val event: String) {

    CARD_TOKENIZE_STARTED("card:tokenize:started"),
    CARD_TOKENIZE_FAILED("card:tokenize:failed"),
    CARD_TOKENIZE_SUCCEEDED("card:tokenize:succeeded")
}
