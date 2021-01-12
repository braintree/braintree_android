package com.braintreepayments.api;

import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockTokenizationClientBuilder {

    private Exception error;
    private PaymentMethodNonce successNonce;

    public MockTokenizationClientBuilder successNonce(PaymentMethodNonce successNonce) {
        this.successNonce = successNonce;
        return this;
    }

    public MockTokenizationClientBuilder error(Exception error) {
        this.error = error;
        return this;
    }

    public TokenizationClient build() {
        TokenizationClient tokenizationClient = mock(TokenizationClient.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                PaymentMethodNonceCallback listener = (PaymentMethodNonceCallback) invocation.getArguments()[1];
                if (successNonce != null) {
                    listener.success(successNonce);
                } else if (error != null) {
                    listener.failure(error);
                }
                return null;
            }
        }).when(tokenizationClient).tokenize(any(PaymentMethodBuilder.class), any(PaymentMethodNonceCallback.class));

        return tokenizationClient;
    }
}
