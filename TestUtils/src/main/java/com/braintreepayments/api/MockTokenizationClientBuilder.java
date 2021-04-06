package com.braintreepayments.api;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockTokenizationClientBuilder {

    private Exception error;
    private BraintreeNonce successNonce;

    public MockTokenizationClientBuilder successNonce(BraintreeNonce successNonce) {
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
            public Void answer(InvocationOnMock invocation) {
                PaymentMethodNonceCallback listener = (PaymentMethodNonceCallback) invocation.getArguments()[1];
                listener.onResult(successNonce, error);
                return null;
            }
        }).when(tokenizationClient).tokenize(any(PaymentMethod.class), any(PaymentMethodNonceCallback.class));

        return tokenizationClient;
    }
}
