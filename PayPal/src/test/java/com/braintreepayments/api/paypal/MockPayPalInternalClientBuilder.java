package com.braintreepayments.api.paypal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import android.content.Context;

import com.braintreepayments.api.core.Configuration;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @deprecated This outputs a Mockito mock of PayPalInternalClient.
 * For writing tests in kotlin, use {@link com.braintreepayments.api.paypal.MockkPayPalInternalClientBuilder} that outputs a mockk mock.
 */
@Deprecated
public class MockPayPalInternalClientBuilder {

    private Exception error;
    private PayPalPaymentAuthRequestParams successResponse;
    private PayPalAccountNonce tokenizeSuccess;

    public MockPayPalInternalClientBuilder sendRequestSuccess(
            PayPalPaymentAuthRequestParams successResponse) {
        this.successResponse = successResponse;
        return this;
    }

    public MockPayPalInternalClientBuilder sendRequestError(Exception error) {
        this.error = error;
        return this;
    }

    public MockPayPalInternalClientBuilder tokenizeSuccess(PayPalAccountNonce tokenizeSuccess) {
        this.tokenizeSuccess = tokenizeSuccess;
        return this;
    }

    public PayPalInternalClient build() {
        PayPalInternalClient payPalInternalClient = mock(PayPalInternalClient.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                PayPalInternalClientCallback callback =
                        (PayPalInternalClientCallback) invocation.getArguments()[3];
                if (successResponse != null) {
                    callback.onResult(successResponse, null);
                } else if (error != null) {
                    callback.onResult(null, error);
                }
                return null;
            }
        }).when(payPalInternalClient).sendRequest(any(Context.class), any(PayPalRequest.class),
                any(Configuration.class), any(PayPalInternalClientCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                PayPalInternalTokenizeCallback callback =
                        (PayPalInternalTokenizeCallback) invocation.getArguments()[1];
                callback.onResult(tokenizeSuccess, null);
                return null;
            }
        }).when(payPalInternalClient)
                .tokenize(any(PayPalAccount.class), any(PayPalInternalTokenizeCallback.class));

        return payPalInternalClient;
    }
}
