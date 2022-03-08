package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class MockSEPADebitApiBuilder {

    private Exception createMandateError;
    private CreateMandateResult createMandateResultSuccess;

    public MockSEPADebitApiBuilder createMandateResultSuccess(CreateMandateResult createMandateResultSuccess) {
        this.createMandateResultSuccess = createMandateResultSuccess;
        return this;
    }

    public MockSEPADebitApiBuilder createMandateError(Exception createMandateError) {
        this.createMandateError = createMandateError;
        return this;
    }

    public SEPADebitApi build() {
        SEPADebitApi sepaDebitApi = mock(SEPADebitApi.class);

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
        }).when(sepaDebitApi).createMandate(any(SEPADebitRequest.class), any(Configuration.class), anyString(), any(CreateMandateCallback.class));

        return sepaDebitApi;
    }
}
