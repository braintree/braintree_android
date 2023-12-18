package com.braintreepayments.api;

import androidx.annotation.Nullable;


interface SEPADirectDebitInternalTokenizeCallback {

    void onResult(@Nullable SEPADirectDebitNonce sepaDirectDebitNonce, @Nullable Exception error);
}
