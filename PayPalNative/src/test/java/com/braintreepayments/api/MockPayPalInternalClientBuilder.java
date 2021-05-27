package com.braintreepayments.api;

import android.content.Context;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockPayPalInternalClientBuilder {

    private Exception error;
    private PayPalResponse successResponse;

    public MockPayPalInternalClientBuilder success(PayPalResponse successResponse) {
        this.successResponse = successResponse;
        return this;
    }

    public MockPayPalInternalClientBuilder error(Exception error) {
        this.error = error;
        return this;
    }

    public PayPalInternalClient build() {
        PayPalInternalClient payPalInternalClient = mock(PayPalInternalClient.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                PayPalInternalClientCallback callback = (PayPalInternalClientCallback) invocation.getArguments()[2];
                if (successResponse != null) {
                    callback.onResult(successResponse, null);
                } else if (error != null) {
                    callback.onResult(null, error);
                }
                return null;
            }
        }).when(payPalInternalClient).sendRequest(any(Context.class), any(PayPalRequest.class), any(PayPalInternalClientCallback.class));

        return payPalInternalClient;
    }
}
