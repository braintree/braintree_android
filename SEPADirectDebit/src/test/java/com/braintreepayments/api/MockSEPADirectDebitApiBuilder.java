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

    public MockSEPADirectDebitApiBuilder createMandateResultSuccess(CreateMandateResult createMandateResultSuccess) {
        this.createMandateResultSuccess = createMandateResultSuccess;
        return this;
    }

    public MockSEPADirectDebitApiBuilder createMandateError(Exception createMandateError) {
        this.createMandateError = createMandateError;
        return this;
    }

    public SEPADirectDebitApi build() {
        SEPADirectDebitApi sepaDirectDebitApi = mock(SEPADirectDebitApi.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                CreateMandateCallback callback = (CreateMandateCallback) invocation.getArguments()[3];
                if (createMandateResultSuccess != null) {
                    callback.onResult(createMandateResultSuccess, null);
                } else if (createMandateError != null) {
                    callback.onResult(null, createMandateError);
                }
                return null;
            }
        }).when(sepaDirectDebitApi).createMandate(any(SEPADirectDebitRequest.class), any(Configuration.class), anyString(), any(CreateMandateCallback.class));

        return sepaDirectDebitApi;
    }
}
