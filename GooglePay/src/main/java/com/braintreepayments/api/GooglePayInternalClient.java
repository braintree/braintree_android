package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

class GooglePayInternalClient {

    private final PaymentsClientWrapper paymentsClientWrapper;

    GooglePayInternalClient() {
        this(new PaymentsClientWrapper());
    }

    @VisibleForTesting
    GooglePayInternalClient(PaymentsClientWrapper paymentsClientWrapper) {
        this.paymentsClientWrapper = paymentsClientWrapper;
    }

    void isReadyToPay(FragmentActivity activity, Configuration configuration, IsReadyToPayRequest isReadyToPayRequest, final GooglePayIsReadyToPayCallback callback) {
        PaymentsClient paymentsClient = paymentsClientWrapper.getPaymentsClient(activity, configuration);
        paymentsClient.isReadyToPay(isReadyToPayRequest).addOnCompleteListener(new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                try {
                    callback.onResult(task.getResult(ApiException.class), null);
                } catch (ApiException e) {
                    callback.onResult(false, e);
                }
            }
        });
    }
}
