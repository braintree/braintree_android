package com.braintreepayments.api.helpers;

import android.content.Context;

import com.braintreepayments.api.PayPalInternalClient;
import com.braintreepayments.api.PayPalInternalClientCallback;
import com.braintreepayments.api.PayPalResponse;
import com.braintreepayments.api.models.PayPalRequest;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
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
                PayPalInternalClientCallback callback = (PayPalInternalClientCallback) invocation.getArguments()[3];
                if (successResponse != null) {
                    callback.onResult(successResponse, null);
                } else if (error != null) {
                    callback.onResult(null, error);
                }
                return null;
            }
        }).when(payPalInternalClient).sendRequest(any(Context.class), any(PayPalRequest.class), anyBoolean(), any(PayPalInternalClientCallback.class));

        return payPalInternalClient;
    }
}
