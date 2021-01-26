package com.braintreepayments.api;

import android.content.Context;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockCardinalClientBuilder {

    private Exception error;
    private String successReferenceId;

    public MockCardinalClientBuilder successReferenceId(String successReferenceId) {
        this.successReferenceId = successReferenceId;
        return this;
    }

    public MockCardinalClientBuilder error(Exception error) {
        this.error = error;
        return this;
    }

    public CardinalClient build() {
        CardinalClient cardinalClient = mock(CardinalClient.class);
        when(cardinalClient.getConsumerSessionId()).thenReturn(successReferenceId);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                CardinalInitializeCallback listener = (CardinalInitializeCallback) invocation.getArguments()[3];
                if (successReferenceId != null) {
                    listener.onResult(successReferenceId, null);
                } else if (error != null) {
                    listener.onResult(null, error);
                }
                return null;
            }
        }).when(cardinalClient).initialize(any(Context.class), any(Configuration.class), any(ThreeDSecureRequest.class), any(CardinalInitializeCallback.class));

        return cardinalClient;
    }
}
