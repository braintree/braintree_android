package com.braintreepayments.api;

import android.content.Context;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockPayPalInternalClientBuilder {

    private Exception error;
    private PayPalNativeCheckoutResponse successResponse;
    private PayPalNativeCheckoutAccountNonce tokenizeSuccess;

    public MockPayPalInternalClientBuilder sendRequestSuccess(PayPalNativeCheckoutResponse successResponse) {
        this.successResponse = successResponse;
        return this;
    }

    public MockPayPalInternalClientBuilder sendRequestError(Exception error) {
        this.error = error;
        return this;
    }

    public MockPayPalInternalClientBuilder tokenizeSuccess(PayPalNativeCheckoutAccountNonce tokenizeSuccess) {
        this.tokenizeSuccess = tokenizeSuccess;
        return this;
    }

    public PayPalNativeCheckoutInternalClient build() {
        PayPalNativeCheckoutInternalClient payPalInternalClient = mock(PayPalNativeCheckoutInternalClient.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                PayPalNativeCheckoutInternalClient.PayPalNativeCheckoutInternalClientCallback callback = (PayPalNativeCheckoutInternalClient.PayPalNativeCheckoutInternalClientCallback) invocation.getArguments()[2];
                if (successResponse != null) {
                    callback.onResult(successResponse, null);
                } else if (error != null) {
                    callback.onResult(null, error);
                }
                return null;
            }
        }).when(payPalInternalClient).sendRequest(any(Context.class), any(PayPalNativeRequest.class), any(PayPalNativeCheckoutInternalClient.PayPalNativeCheckoutInternalClientCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                PayPalNativeCheckoutResultCallback callback = (PayPalNativeCheckoutResultCallback) invocation.getArguments()[1];
                callback.onResult(tokenizeSuccess, null);
                return null;
            }
        }).when(payPalInternalClient).tokenize(any(PayPalNativeCheckoutAccount.class), any(PayPalNativeCheckoutResultCallback.class));

        return payPalInternalClient;
    }
}
