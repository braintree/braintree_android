package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

public class SEPADirectDebitResponse {

    private BrowserSwitchOptions browserSwitchOptions;
    private SEPADirectDebitNonce sepaDirectDebitNonce;

    SEPADirectDebitResponse(BrowserSwitchOptions browserSwitchOptions, SEPADirectDebitNonce sepaDirectDebitNonce) {
        this.browserSwitchOptions = browserSwitchOptions;
        this.sepaDirectDebitNonce = sepaDirectDebitNonce;
    }

    BrowserSwitchOptions getBrowserSwitchOptions() {
        return browserSwitchOptions;
    }

    /**
     * If this nonce is non-null, then the SEPA mandate is already approved.
     * If this nonce is null, continue to present the SEPA mandate web flow via
     * {@link SEPADirectDebitLauncher#launch(FragmentActivity, SEPADirectDebitResponse)}
     *
     * @return {@link SEPADirectDebitNonce}
     */
    public SEPADirectDebitNonce getNonce() {
        return this.sepaDirectDebitNonce;
    }
}
