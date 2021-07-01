package com.braintreepayments.api;

import android.os.Bundle;

import com.samsung.android.sdk.samsungpay.v2.SamsungPay;
import com.samsung.android.sdk.samsungpay.v2.StatusListener;
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockPaymentManagerBuilder {

    private Integer errorCode;
    private List<CardInfo> requestCardInfoSuccess;

    MockPaymentManagerBuilder requestCardInfoSuccess(List<CardInfo> requestCardInfoSuccess) {
        this.requestCardInfoSuccess = requestCardInfoSuccess;
        return this;
    }

    MockPaymentManagerBuilder requestCardInfoErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    PaymentManager build() {
        PaymentManager paymentManager = mock(PaymentManager.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                PaymentManager.CardInfoListener callback =
                    (PaymentManager.CardInfoListener) invocation.getArguments()[1];

                if (requestCardInfoSuccess != null) {
                    callback.onResult(requestCardInfoSuccess);
                } else if (errorCode != null) {
                    callback.onFailure(errorCode, new Bundle());
                }
                return null;
            }
        }).when(paymentManager).requestCardInfo(any(Bundle.class), any(PaymentManager.CardInfoListener.class));

        return paymentManager;
    }
}
