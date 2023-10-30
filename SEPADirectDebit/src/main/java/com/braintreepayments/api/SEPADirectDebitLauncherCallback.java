package com.braintreepayments.api;

import androidx.annotation.NonNull;

public interface SEPADirectDebitLauncherCallback {

    void onResult(@NonNull SEPADirectDebitBrowserSwitchResult sepaDirectDebitBrowserSwitchResult);
}
