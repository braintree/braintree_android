package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.samsung.android.sdk.samsungpay.v2.SpaySdk;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;

public class MockSamsungPayInternalClientBuilder {

    private Integer getSamsungPayStatusSuccess;
    private Exception getSamsungPayStatusError;

    private List<SpaySdk.Brand> getAcceptedCardBrandsSuccess;
    private Exception getAcceptedCardBrandsError;

    MockSamsungPayInternalClientBuilder getSamsungPayStatusSuccess(int getSamsungPayStatusSuccess) {
        this.getSamsungPayStatusSuccess = getSamsungPayStatusSuccess;
        return this;
    }

    MockSamsungPayInternalClientBuilder getSamsungPayStatusError(Exception getSamsungPayStatusError) {
        this.getSamsungPayStatusError = getSamsungPayStatusError;
        return this;
    }

    MockSamsungPayInternalClientBuilder getAcceptedCardBrandsSuccess(List<SpaySdk.Brand> getAcceptedCardBrandsSuccess) {
        this.getAcceptedCardBrandsSuccess = getAcceptedCardBrandsSuccess;
        return this;
    }

    MockSamsungPayInternalClientBuilder getAcceptedCardBrandsError(Exception getAcceptedCardBrandsError) {
        this.getAcceptedCardBrandsError = getAcceptedCardBrandsError;
        return this;
    }

    SamsungPayInternalClient build() {
        SamsungPayInternalClient internalClient = mock(SamsungPayInternalClient.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                GetSamsungPayStatusCallback callback = (GetSamsungPayStatusCallback) invocation.getArguments()[0];
                callback.onResult(getSamsungPayStatusSuccess, getSamsungPayStatusError);
                return null;
            }
        }).when(internalClient).getSamsungPayStatus(any(GetSamsungPayStatusCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                GetAcceptedCardBrandsCallback callback = (GetAcceptedCardBrandsCallback) invocation.getArguments()[0];
                if (getAcceptedCardBrandsSuccess != null) {
                    callback.onResult(getAcceptedCardBrandsSuccess, null);
                } else if (getAcceptedCardBrandsError != null) {
                    callback.onResult(null, getAcceptedCardBrandsError);
                }
                return null;
            }
        }).when(internalClient).getAcceptedCardBrands(any(GetAcceptedCardBrandsCallback.class));

        return internalClient;
    }
}
