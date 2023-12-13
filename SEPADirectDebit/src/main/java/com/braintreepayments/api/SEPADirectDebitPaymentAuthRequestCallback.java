package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link SEPADirectDebitClient#createPaymentAuthRequest(SEPADirectDebitRequest, SEPADirectDebitPaymentAuthRequestCallback)}.
 */
public interface SEPADirectDebitPaymentAuthRequestCallback {

    /**
     * @param paymentAuthRequest the result of the SEPA create mandate call. If a nonce is present,
     *                           no web-based mandate is required. If a nonce is not present, you
     *                           must trigger the web-based mandate flow via
     *                           {@link SEPADirectDebitLauncher#launch(FragmentActivity,
     *                           SEPADirectDebitPaymentAuthRequestParams)}
     */
    void onResult(SEPADirectDebitPaymentAuthRequest paymentAuthRequest);
}
