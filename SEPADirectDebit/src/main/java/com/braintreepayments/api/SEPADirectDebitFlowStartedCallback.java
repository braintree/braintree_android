package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link SEPADirectDebitClient#tokenize(SEPADirectDebitRequest, SEPADirectDebitFlowStartedCallback)}.
 */
public interface SEPADirectDebitFlowStartedCallback {

    /**
     * @param sepaDirectDebitResponse the result of the SEPA create mandate call. If a nonce is present,
     *                                no web-based mandate is required. If a nonce is not present,
     *                                you must trigger the web-based mandate flow via
     *                                {@link SEPADirectDebitLauncher#launch(FragmentActivity, SEPADirectDebitResponse)}
     * @param error                   an exception that occurred while initiating the SEPA transaction
     */
    void onResult(@Nullable SEPADirectDebitResponse sepaDirectDebitResponse, @Nullable Exception error);
}
