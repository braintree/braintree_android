package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Returned via the {@link SEPADirectDebitPaymentAuthRequestCallback} after calling
 * {@link SEPADirectDebitClient#createPaymentAuthRequest(SEPADirectDebitRequest, SEPADirectDebitPaymentAuthRequestCallback)}.
 *
 * Inspect the {@link SEPADirectDebitNonce} property to determine if tokenization is complete, or
 * if you must continue the SEPA mandate web flow via
 * {@link SEPADirectDebitLauncher#launch(FragmentActivity, SEPADirectDebitPaymentAuthRequestParams)}
 */
public class SEPADirectDebitPaymentAuthRequestParams {

    private BrowserSwitchOptions browserSwitchOptions;
    private SEPADirectDebitNonce sepaDirectDebitNonce;

    SEPADirectDebitPaymentAuthRequestParams(BrowserSwitchOptions browserSwitchOptions, SEPADirectDebitNonce sepaDirectDebitNonce) {
        this.browserSwitchOptions = browserSwitchOptions;
        this.sepaDirectDebitNonce = sepaDirectDebitNonce;
    }

    BrowserSwitchOptions getBrowserSwitchOptions() {
        return browserSwitchOptions;
    }

    /**
     * If this nonce is non-null, then the SEPA mandate is already approved.
     * If this nonce is null, continue to present the SEPA mandate web flow via
     * {@link SEPADirectDebitLauncher#launch(FragmentActivity, SEPADirectDebitPaymentAuthRequestParams)}
     *
     * @return {@link SEPADirectDebitNonce}
     */
    public @Nullable SEPADirectDebitNonce getNonce() {
        return this.sepaDirectDebitNonce;
    }
}
