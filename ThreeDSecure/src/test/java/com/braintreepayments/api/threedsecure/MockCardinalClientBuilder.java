package com.braintreepayments.api.threedsecure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.Configuration;

import org.mockito.stubbing.Answer;

public class MockCardinalClientBuilder {

    private Exception error;
    private BraintreeException initializeRuntimeError;
    private String successReferenceId;

    public MockCardinalClientBuilder successReferenceId(String successReferenceId) {
        this.successReferenceId = successReferenceId;
        return this;
    }

    public MockCardinalClientBuilder error(Exception error) {
        this.error = error;
        return this;
    }

    public MockCardinalClientBuilder initializeRuntimeError(
            BraintreeException initializeRuntimeError) {
        this.initializeRuntimeError = initializeRuntimeError;
        return this;
    }

    public CardinalClient build() throws BraintreeException {
        CardinalClient cardinalClient = mock(CardinalClient.class);
        when(cardinalClient.getConsumerSessionId()).thenReturn(successReferenceId);

        if (initializeRuntimeError != null) {
            doThrow(initializeRuntimeError)
                    .when(cardinalClient)
                    .initialize(any(Context.class), any(Configuration.class),
                            any(ThreeDSecureRequest.class), any(CardinalInitializeCallback.class));
        } else {
            doAnswer((Answer<Void>) invocation -> {
                CardinalInitializeCallback listener =
                        (CardinalInitializeCallback) invocation.getArguments()[3];
                if (successReferenceId != null) {
                    listener.onResult(successReferenceId, null);
                } else if (error != null) {
                    listener.onResult(null, error);
                }
                return null;
            }).when(cardinalClient).initialize(any(Context.class), any(Configuration.class),
                    any(ThreeDSecureRequest.class), any(CardinalInitializeCallback.class));

        }

        return cardinalClient;
    }
}
