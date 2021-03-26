package com.braintreepayments.api;

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
                TokenizeCallback listener = (TokenizeCallback) invocation.getArguments()[1];
                if (successNonce != null) {
                    listener.onResult(successNonce, );
                } else if (error != null) {
                    listener.failure(error);
                }
                return null;
            }
        }).when(tokenizationClient).tokenize(any(PaymentMethodBuilder.class), any(TokenizeCallback.class));

        return tokenizationClient;
    }
}
