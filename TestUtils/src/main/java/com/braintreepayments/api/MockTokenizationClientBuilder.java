package com.braintreepayments.api;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockTokenizationClientBuilder {

    private Exception error;
    private String successResponse;

    public MockTokenizationClientBuilder successResponse(String successResponse) {
        this.successResponse = successResponse;
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
                if (successResponse != null) {
                    listener.success(successResponse);
                } else if (error != null) {
                    listener.failure(error);
                }
                return null;
            }
        }).when(tokenizationClient).tokenize(any(PaymentMethod.class), any(PaymentMethodNonceCallback.class));

        return tokenizationClient;
    }
}
