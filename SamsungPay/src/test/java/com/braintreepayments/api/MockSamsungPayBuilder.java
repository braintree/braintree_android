package com.braintreepayments.api;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.samsung.android.sdk.samsungpay.v2.SamsungPay;
import com.samsung.android.sdk.samsungpay.v2.StatusListener;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class MockSamsungPayBuilder {

    private Integer errorCode;
    private Integer successStatusCode;

    MockSamsungPayBuilder errorCode(Integer errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    MockSamsungPayBuilder successStatusCode(Integer successStatusCode) {
        this.successStatusCode = successStatusCode;
        return this;
    }

    SamsungPay build() {
        SamsungPay samsungPay = mock(SamsungPay.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                StatusListener callback = (StatusListener) invocation.getArguments()[0];
                if (successStatusCode != null) {
                    callback.onSuccess(successStatusCode, new Bundle());
                } else if (errorCode != null) {
                    callback.onFail(errorCode, new Bundle());
                }
                return null;
            }
        }).when(samsungPay).getSamsungPayStatus(any(StatusListener.class));

        return samsungPay;
    }
}
