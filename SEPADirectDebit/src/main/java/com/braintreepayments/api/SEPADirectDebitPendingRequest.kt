package com.braintreepayments.api

import java.lang.Exception

sealed class SEPADirectDebitPendingRequest {

    class Started(val request: SEPADirectDebitBrowserSwitchRequest) : SEPADirectDebitPendingRequest()

    class Failure(val error: Exception) : SEPADirectDebitPendingRequest()
}
