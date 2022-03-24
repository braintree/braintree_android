package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class MockSEPADirectDebitApiBuilder {

    private Exception createMandateError;
    private CreateMandateResult createMandateResultSuccess;
    private Exception tokenizeError;
    private SEPADirectDebitNonce tokenizeSuccess;

    public MockSEPADirectDebitApiBuilder createMandateResultSuccess(CreateMandateResult createMandateResultSuccess) {
        this.createMandateResultSuccess = createMandateResultSuccess;
        return this;
    }

    public MockSEPADirectDebitApiBuilder createMandateError(Exception createMandateError) {
        this.createMandateError = createMandateError;
        return this;
    }

    public MockSEPADirectDebitApiBuilder tokenizeSuccess(SEPADirectDebitNonce tokenizeSuccess) {
        this.tokenizeSuccess = tokenizeSuccess;
        return this;
    }

    public MockSEPADirectDebitApiBuilder tokenizeError(Exception tokenizeError) {
        this.tokenizeError = tokenizeError;
        return this;
    }

    public SEPADirectDebitApi build() {
        SEPADirectDebitApi sepaDirectDebitApi = mock(SEPADirectDebitApi.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                CreateMandateCallback callback = (CreateMandateCallback) invocation.getArguments()[2];
                if (createMandateResultSuccess != null) {
                    callback.onResult(createMandateResultSuccess, null);
                } else if (createMandateError != null) {
                    callback.onResult(null, createMandateError);
                }
                return null;
            }
        }).when(sepaDirectDebitApi).createMandate(any(SEPADirectDebitRequest.class), anyString(), any(CreateMandateCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                SEPADirectDebitTokenizeCallback callback = (SEPADirectDebitTokenizeCallback) invocation.getArguments()[4];
                if (tokenizeSuccess != null) {
                    callback.onResult(tokenizeSuccess, null);
                } else if (tokenizeError != null) {
                    callback.onResult(null, tokenizeError);
                }
                return null;
            }
        }).when(sepaDirectDebitApi).tokenize(anyString(), anyString(), anyString(), anyString(), any(SEPADirectDebitTokenizeCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                SEPADirectDebitTokenizeCallback callback = (SEPADirectDebitTokenizeCallback) invocation.getArguments()[4];
                if (tokenizeSuccess != null) {
                    callback.onResult(tokenizeSuccess, null);
                } else if (tokenizeError != null) {
                    callback.onResult(null, tokenizeError);
                }
                return null;
            }
        }).when(sepaDirectDebitApi).tokenize(anyString(), anyString(), anyString(), anyString(), any(SEPADirectDebitTokenizeCallback.class));

        return sepaDirectDebitApi;
    }
}
