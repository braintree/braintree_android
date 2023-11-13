package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.mockito.stubbing.Answer;

public class MockLocalPaymentApiBuilder {

    private LocalPaymentNonce tokenizeSuccess;
    private Exception tokenizeError;
    private LocalPaymentAuthRequest createPaymentMethodSuccess;
    private Exception createPaymentMethodError;

    public MockLocalPaymentApiBuilder tokenizeSuccess(LocalPaymentNonce tokenizeSuccess) {
        this.tokenizeSuccess = tokenizeSuccess;
        return this;
    }

    public MockLocalPaymentApiBuilder tokenizeError(Exception error) {
        this.tokenizeError = error;
        return this;
    }

    public MockLocalPaymentApiBuilder createPaymentMethodSuccess(
            LocalPaymentAuthRequest createPaymentMethodSuccess) {
        this.createPaymentMethodSuccess = createPaymentMethodSuccess;
        return this;
    }

    public MockLocalPaymentApiBuilder createPaymentMethodError(Exception error) {
        this.createPaymentMethodError = error;
        return this;
    }

    public LocalPaymentApi build() {
        LocalPaymentApi localPaymentAPI = mock(LocalPaymentApi.class);

        doAnswer((Answer<Void>) invocation -> {
            LocalPaymentTokenizeCallback callback =
                    (LocalPaymentTokenizeCallback) invocation.getArguments()[3];
            if (tokenizeSuccess != null) {
                callback.onResult(tokenizeSuccess, null);
            } else if (tokenizeError != null) {
                callback.onResult(null, tokenizeError);
            }
            return null;
        }).when(localPaymentAPI).tokenize(anyString(), anyString(), anyString(),
                any(LocalPaymentTokenizeCallback.class));

        doAnswer((Answer<Void>) invocation -> {
            LocalPaymentAuthRequestCallback callback =
                    (LocalPaymentAuthRequestCallback) invocation.getArguments()[1];
            if (createPaymentMethodSuccess != null) {
                callback.onResult(createPaymentMethodSuccess, null);
            } else if (createPaymentMethodError != null) {
                callback.onResult(null, createPaymentMethodError);
            }
            return null;
        }).when(localPaymentAPI).createPaymentMethod(any(LocalPaymentRequest.class),
                any(LocalPaymentAuthRequestCallback.class));

        return localPaymentAPI;
    }
}
