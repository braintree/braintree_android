package com.braintreepayments.api;

import org.json.JSONObject;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockApiClientBuilder {

    private Exception tokenizeRESTError;
    private JSONObject tokenizeRESTSuccess;

    private Exception tokenizeGraphQLError;
    private JSONObject tokenizeGraphQLSuccess;

    public MockApiClientBuilder tokenizeRESTError(Exception tokenizeRestError) {
        this.tokenizeRESTError = tokenizeRestError;
        return this;
    }

    public MockApiClientBuilder tokenizeRESTSuccess(JSONObject tokenizeRestSuccess) {
        this.tokenizeRESTSuccess = tokenizeRestSuccess;
        return this;
    }

    public MockApiClientBuilder tokenizeGraphQLError(Exception tokenizeGraphQLError) {
        this.tokenizeGraphQLError = tokenizeGraphQLError;
        return this;
    }

    public MockApiClientBuilder tokenizeGraphQLSuccess(JSONObject tokenizeGraphQLSuccess) {
        this.tokenizeGraphQLSuccess = tokenizeGraphQLSuccess;
        return this;
    }

    public ApiClient build() {
        ApiClient apiClient = mock(ApiClient.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                TokenizeCallback listener = (TokenizeCallback) invocation.getArguments()[1];
                listener.onResult(tokenizeRESTSuccess, tokenizeRESTError);
                return null;
            }
        }).when(apiClient).tokenizeREST(any(PaymentMethod.class), any(TokenizeCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                TokenizeCallback listener = (TokenizeCallback) invocation.getArguments()[1];
                listener.onResult(tokenizeGraphQLSuccess, tokenizeGraphQLError);
                return null;
            }
        }).when(apiClient).tokenizeGraphQL(any(JSONObject.class), any(TokenizeCallback.class));

        return apiClient;
    }
}
