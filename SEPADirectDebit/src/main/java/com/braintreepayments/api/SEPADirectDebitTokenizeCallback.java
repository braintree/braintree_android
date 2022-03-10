package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface SEPADirectDebitTokenizeCallback {

    void onResult(@Nullable SEPADirectDebitNonce sepaDirectDebitNonce, @Nullable Exception error);

}
