package com.braintreepayments.api;

import org.json.JSONObject;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockTokenizationClientBuilder {

    private Exception tokenizeRESTError;
    private JSONObject tokenizeRESTSuccess;

    private Exception tokenizeGraphQLError;
    private JSONObject tokenizeGraphQLSuccess;

    public MockTokenizationClientBuilder tokenizeRESTError(Exception tokenizeRestError) {
        this.tokenizeRESTError = tokenizeRestError;
        return this;
    }

    public MockTokenizationClientBuilder tokenizeRESTSuccess(JSONObject tokenizeRestSuccess) {
        this.tokenizeRESTSuccess = tokenizeRestSuccess;
        return this;
    }

    public MockTokenizationClientBuilder tokenizeGraphQLError(Exception tokenizeGraphQLError) {
        this.tokenizeGraphQLError = tokenizeGraphQLError;
        return this;
    }

    public MockTokenizationClientBuilder tokenizeGraphQLSuccess(JSONObject tokenizeGraphQLSuccess) {
        this.tokenizeGraphQLSuccess = tokenizeGraphQLSuccess;
        return this;
    }

    public APIClient build() {
        APIClient apiClient = mock(APIClient.class);

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
