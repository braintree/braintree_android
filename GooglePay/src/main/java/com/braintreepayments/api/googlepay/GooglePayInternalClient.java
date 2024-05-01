package com.braintreepayments.api.googlepay;

import android.content.Context;
<<<<<<< HEAD:GooglePay/src/main/java/com/braintreepayments/api/googlepay/GooglePayInternalClient.java
=======

import androidx.annotation.NonNull;
>>>>>>> main:GooglePay/src/main/java/com/braintreepayments/api/GooglePayInternalClient.java

import com.braintreepayments.api.core.Configuration;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

class GooglePayInternalClient {

    void isReadyToPay(Context context, Configuration configuration, IsReadyToPayRequest isReadyToPayRequest, final GooglePayIsReadyToPayCallback callback) {
        PaymentsClient paymentsClient = Wallet.getPaymentsClient(context,
                new Wallet.WalletOptions.Builder()
                        .setEnvironment(getGooglePayEnvironment(configuration))
                        .build());
        paymentsClient.isReadyToPay(isReadyToPayRequest).addOnCompleteListener(task -> {
            try {
                Boolean isReady = task.getResult(ApiException.class);
                if (isReady) {
                    callback.onGooglePayReadinessResult(GooglePayReadinessResult.ReadyToPay.INSTANCE);
                } else {
                    callback.onGooglePayReadinessResult(new GooglePayReadinessResult.NotReadyToPay(null));
                }
            } catch (ApiException e) {
                callback.onGooglePayReadinessResult(new GooglePayReadinessResult.NotReadyToPay(e));
            }
        });
    }

    int getGooglePayEnvironment(Configuration configuration) {
        if ("production".equals(configuration.getGooglePayEnvironment())) {
            return WalletConstants.ENVIRONMENT_PRODUCTION;
        } else {
            return WalletConstants.ENVIRONMENT_TEST;
        }
    }
}
