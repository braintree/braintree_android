package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class MockClientTokenProviderBuilder {

    private String clientToken;
    private Exception error;

    public MockClientTokenProviderBuilder clientToken(String clientToken) {
        this.clientToken = clientToken;
        return this;
    }

    public MockClientTokenProviderBuilder error(Exception error) {
        this.error = error;
        return this;
    }

    ClientTokenProvider build() {
        ClientTokenProvider clientTokenProvider = mock(ClientTokenProvider.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ClientTokenCallback callback = (ClientTokenCallback) invocation.getArguments()[0];
                if (clientToken != null) {
                    callback.onSuccess(clientToken);
                } else if (error != null) {
                    callback.onFailure(error);
                }
                return null;
            }
        }).when(clientTokenProvider).getClientToken(any(ClientTokenCallback.class));

        return clientTokenProvider;
    }
}
