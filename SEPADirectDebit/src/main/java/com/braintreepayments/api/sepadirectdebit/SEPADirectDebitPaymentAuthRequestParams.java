package com.braintreepayments.api.sepadirectdebit;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BrowserSwitchOptions;

/**
 * Returned via the {@link SEPADirectDebitPaymentAuthRequestCallback} after calling
 * {@link SEPADirectDebitClient#createPaymentAuthRequest(SEPADirectDebitRequest, SEPADirectDebitPaymentAuthRequestCallback)}.
 *
 * Inspect the {@link SEPADirectDebitNonce} property to determine if tokenization is complete, or
 * if you must continue the SEPA mandate web flow via
 * {@link SEPADirectDebitLauncher#launch(FragmentActivity, SEPADirectDebitPaymentAuthRequest.ReadyToLaunch)}
 */
public class SEPADirectDebitPaymentAuthRequestParams {

    private BrowserSwitchOptions browserSwitchOptions;

    SEPADirectDebitPaymentAuthRequestParams(BrowserSwitchOptions browserSwitchOptions) {
        this.browserSwitchOptions = browserSwitchOptions;
    }

    BrowserSwitchOptions getBrowserSwitchOptions() {
        return browserSwitchOptions;
    }

}
