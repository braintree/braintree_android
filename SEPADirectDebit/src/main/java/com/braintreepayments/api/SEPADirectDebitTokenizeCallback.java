package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link SEPADirectDebitClient#tokenize(SEPADirectDebitPaymentAuthResult, SEPADirectDebitTokenizeCallback)}.
 */
public interface SEPADirectDebitTokenizeCallback {

    /**
     * @param sepaDirectDebitNonce {@link SEPADirectDebitNonce}
     * @param error                an exception that occurred while processing a PayPal result
     */
    void onResult(@Nullable SEPADirectDebitNonce sepaDirectDebitNonce, @Nullable Exception error);
}
