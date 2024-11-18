package com.braintreepayments.api.venmo

internal object VenmoAnalytics {

    // Conversion Events
    const val TOKENIZE_STARTED = "venmo:tokenize:started"
    const val TOKENIZE_FAILED = "venmo:tokenize:failed"
    const val TOKENIZE_SUCCEEDED = "venmo:tokenize:succeeded"
    const val APP_SWITCH_CANCELED = "venmo:tokenize:app-switch:canceled"

    // Additional Detail Events
    const val APP_SWITCH_SUCCEEDED = "venmo:tokenize:app-switch:succeeded"
    const val APP_SWITCH_FAILED = "venmo:tokenize:app-switch:failed"

    // App Switch events
    const val APP_SWITCH_STARTED = "venmo:tokenize:app-switch:started"
    const val HANDLE_RETURN_STARTED = "venmo:tokenize:handle-return:started"
}
