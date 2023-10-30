package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface SEPADirectDebitFlowStartedCallback {

    void onResult(SEPADirectDebitResponse sepaDirectDebitResponse, @Nullable Exception error);
}
