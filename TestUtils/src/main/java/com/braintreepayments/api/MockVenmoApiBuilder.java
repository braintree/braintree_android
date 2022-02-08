package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class MockVenmoApiBuilder {

    private String venmoPaymentContextId;
    private Exception createPaymentContextError;

    public MockVenmoApiBuilder createPaymentContextSuccess(String venmoPaymentContextId) {
        this.venmoPaymentContextId = venmoPaymentContextId;
        return this;
    }
    public MockVenmoApiBuilder createPaymentContextError(Exception createPaymentContextError) {
        this.createPaymentContextError = createPaymentContextError;
        return this;
    }

    public VenmoAPI build() {
        VenmoAPI venmoAPI = mock(VenmoAPI.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                VenmoApiCallback callback = (VenmoApiCallback) invocation.getArguments()[2];
                if (venmoPaymentContextId != null) {
                    callback.onResult(venmoPaymentContextId, null);
                } else if(createPaymentContextError != null) {
                    callback.onResult(null, createPaymentContextError);
                }

                return null;
            }
        }).when(venmoAPI).createPaymentContext(any(VenmoRequest.class), anyString(), any(VenmoApiCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                VenmoApiCallback callback = (VenmoApiCallback) invocation.getArguments()[2];
                if (venmoPaymentContextId != null) {
                    callback.onResult(venmoPaymentContextId, null);
                } else if(createPaymentContextError != null) {
                    callback.onResult(null, createPaymentContextError);
                }

                return null;
            }
        }).when(venmoAPI).createPaymentContext(any(VenmoRequest.class), anyString(), any(VenmoApiCallback.class));

        return venmoAPI;
    }
}
