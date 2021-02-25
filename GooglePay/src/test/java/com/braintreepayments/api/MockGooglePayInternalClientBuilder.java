package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.wallet.IsReadyToPayRequest;

import org.mockito.invocation.InvocationOnMock;
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

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                GooglePayIsReadyToPayCallback callback = (GooglePayIsReadyToPayCallback) invocation.getArguments()[3];
                if (isReadyToPayError != null) {
                    callback.onResult(null, isReadyToPayError);
                } else {
                    callback.onResult(isReadyToPay, null);
                }
                return null;
            }
        }).when(googlePayInternalClient).isReadyToPay(any(FragmentActivity.class), any(Configuration.class), any(IsReadyToPayRequest.class), any(GooglePayIsReadyToPayCallback.class));

        return googlePayInternalClient;
    }
}
