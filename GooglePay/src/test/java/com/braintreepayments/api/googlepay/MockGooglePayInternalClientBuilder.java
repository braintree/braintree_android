package com.braintreepayments.api.googlepay;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.core.Configuration;
import com.google.android.gms.wallet.IsReadyToPayRequest;

import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class MockGooglePayInternalClientBuilder {

    private boolean isReadyToPay;
    private Exception isReadyToPayError;

    MockGooglePayInternalClientBuilder isReadyToPay(boolean isReadyToPay) {
        this.isReadyToPay = isReadyToPay;
        return this;
    }

    MockGooglePayInternalClientBuilder isReadyToPayError(Exception isReadyToPayError) {
        this.isReadyToPayError = isReadyToPayError;
        return this;
    }

    GooglePayInternalClient build() {
        GooglePayInternalClient googlePayInternalClient = mock(GooglePayInternalClient.class);

        doAnswer((Answer<Void>) invocation -> {
            GooglePayIsReadyToPayCallback callback = (GooglePayIsReadyToPayCallback) invocation.getArguments()[3];
            if (isReadyToPayError != null) {
                callback.onGooglePayReadinessResult(new GooglePayReadinessResult.NotReadyToPay(isReadyToPayError));
            } else {
                callback.onGooglePayReadinessResult(GooglePayReadinessResult.ReadyToPay.INSTANCE);
            }
            return null;
        }).when(googlePayInternalClient).isReadyToPay(any(FragmentActivity.class), any(Configuration.class), any(IsReadyToPayRequest.class), any(GooglePayIsReadyToPayCallback.class));

        return googlePayInternalClient;
    }
}
