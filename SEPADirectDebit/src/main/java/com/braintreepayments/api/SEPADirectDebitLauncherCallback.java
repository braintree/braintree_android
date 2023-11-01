package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Used to receive notification that the SEPA mandate web browser flow completed.
 * Once this is invoked, continue the flow by calling
 * {@link SEPADirectDebitClient#onBrowserSwitchResult(SEPADirectDebitBrowserSwitchResult, SEPADirectDebitBrowserSwitchResultCallback)}
 */
public interface SEPADirectDebitLauncherCallback {

    void onResult(@NonNull SEPADirectDebitBrowserSwitchResult sepaDirectDebitBrowserSwitchResult);
}
