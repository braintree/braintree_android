package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockAuthorizationProviderBuilder {

    private List<String> clientTokens;
    private Exception error;

    public MockAuthorizationProviderBuilder clientToken(String... clientTokens) {
        this.clientTokens = new ArrayList<>(Arrays.asList(clientTokens));
        return this;
    }

    public MockAuthorizationProviderBuilder error(Exception error) {
        this.error = error;
        return this;
    }

    ClientTokenProvider build() {
        ClientTokenProvider clientTokenProvider = mock(ClientTokenProvider.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ClientTokenCallback callback = (ClientTokenCallback) invocation.getArguments()[0];
                if (clientTokens != null) {
                    callback.onSuccess(clientTokens.get(0));

                    // shift array until one item left, at which point all subsequent calls
                    // will return the last item
                    if (clientTokens.size() - 1 > 0) {
                        clientTokens.remove(0);
                    }

                } else if (error != null) {
                    callback.onFailure(error);
                }
                return null;
            }
        }).when(clientTokenProvider).getClientToken(any(ClientTokenCallback.class));

        return clientTokenProvider;
    }
}
