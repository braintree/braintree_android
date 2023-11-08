package com.braintreepayments.api

internal enum class VenmoAnalytics(@JvmField val event: String) {
    // Conversion Events
    TOKENIZE_STARTED("venmo:tokenize:started"),
    TOKENIZE_FAILED("venmo:tokenize:failed"),
    TOKENIZE_SUCCEEDED("venmo:tokenize:succeeded"),
    APP_SWITCH_CANCELED("venmo:tokenize:app-switch:canceled"),

    // Additional Detail Events
    TOKENIZE_NETWORK_CONNECTION_LOST("venmo:tokenize:network-connection:failed"),
    APP_SWITCH_SUCCEEDED("venmo:tokenize:app-switch:succeeded"),
    APP_SWITCH_FAILED("venmo:tokenize:app-switch:failed")
}
