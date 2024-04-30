package com.braintreepayments.api

enum class VenmoAnalyticEvents(val value: String) {
    TOKENIZE_INVOKED("android:venmo:invoked"),
    TOKENIZE_STARTED("venmo:tokenize:started"),
    TOKENIZE_FAILED("venmo:tokenize:failed"),
    TOKENIZE_SUCCEEDED("venmo:tokenize:succeeded"),
    TOKENIZE_CANCELED("venmo:tokenize:app-switch:canceled"),

    APP_SWITCH_SUCCEEDED("venmo:tokenize:app-switch:succeeded"),
    APP_SWITCH_FAILED("venmo:tokenize:app-switch:failed"),
}