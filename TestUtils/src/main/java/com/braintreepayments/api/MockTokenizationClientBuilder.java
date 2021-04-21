package com.braintreepayments.api;

import org.json.JSONObject;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockTokenizationClientBuilder {

    private Exception error;
    private JSONObject successResponse;

    public MockTokenizationClientBuilder successResponse(JSONObject successResponse) {
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
            public Void answer(InvocationOnMock invocation) {
                TokenizeCallback listener = (TokenizeCallback) invocation.getArguments()[1];
                listener.onResult(successResponse, error);
                return null;
            }
        }).when(tokenizationClient).tokenize(any(PaymentMethod.class), any(TokenizeCallback.class));

        return tokenizationClient;
    }
}
