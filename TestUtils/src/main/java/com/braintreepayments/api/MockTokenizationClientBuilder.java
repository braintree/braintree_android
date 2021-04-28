package com.braintreepayments.api;

import org.json.JSONObject;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockTokenizationClientBuilder {

    private Exception tokenizeRestError;
    private JSONObject tokenizeRestSuccessResponse;

    private Exception tokenizeGraphQLError;
    private JSONObject tokenizeGraphQLSuccessResponse;

    public MockTokenizationClientBuilder setTokenizeRestError(Exception tokenizeRestError) {
        this.tokenizeRestError = tokenizeRestError;
        return this;
    }

    public MockTokenizationClientBuilder setTokenizeRestSuccessResponse(JSONObject tokenizeRestSuccessResponse) {
        this.tokenizeRestSuccessResponse = tokenizeRestSuccessResponse;
        return this;
    }

    public MockTokenizationClientBuilder setTokenizeGraphQLError(Exception tokenizeGraphQLError) {
        this.tokenizeGraphQLError = tokenizeGraphQLError;
        return this;
    }

    public MockTokenizationClientBuilder setTokenizeGraphQLSuccessResponse(JSONObject tokenizeGraphQLSuccessResponse) {
        this.tokenizeGraphQLSuccessResponse = tokenizeGraphQLSuccessResponse;
        return this;
    }

    public TokenizationClient build() {
        TokenizationClient tokenizationClient = mock(TokenizationClient.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                TokenizeCallback listener = (TokenizeCallback) invocation.getArguments()[1];
                listener.onResult(tokenizeRestSuccessResponse, tokenizeRestError);
                return null;
            }
        }).when(tokenizationClient).tokenizeREST(any(PaymentMethod.class), any(TokenizeCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                TokenizeCallback listener = (TokenizeCallback) invocation.getArguments()[1];
                listener.onResult(tokenizeGraphQLSuccessResponse, tokenizeGraphQLError);
                return null;
            }
        }).when(tokenizationClient).tokenizeGraphQL(any(JSONObject.class), any(TokenizeCallback.class));

        return tokenizationClient;
    }
}
