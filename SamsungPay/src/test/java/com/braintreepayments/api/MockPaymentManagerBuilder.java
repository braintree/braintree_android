package com.braintreepayments.api;

import android.os.Bundle;

import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockPaymentManagerBuilder {

    private Integer errorCode;
    private List<CardInfo> requestCardInfoSuccess;

    private CardInfo cardInfo;
    private CustomSheet customSheet;

    private Integer startInAppPayWithCustomSheetError;
    private String startInAppPayWithCustomSheetSuccess;
    private CustomSheetPaymentInfo startInAppPayWithCustomSheetPaymentInfo;

    MockPaymentManagerBuilder requestCardInfoSuccess(List<CardInfo> requestCardInfoSuccess) {
        this.requestCardInfoSuccess = requestCardInfoSuccess;
        return this;
    }

    MockPaymentManagerBuilder requestCardInfoErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    MockPaymentManagerBuilder startInAppPayWithCustomSheetCardInfoUpdated(CardInfo cardInfo, CustomSheet customSheet) {
        this.cardInfo = cardInfo;
        this.customSheet = customSheet;
        return this;
    }

    MockPaymentManagerBuilder startInAppPayWithCustomSheetSuccess(CustomSheetPaymentInfo paymentInfo, String success) {
        this.startInAppPayWithCustomSheetPaymentInfo = paymentInfo;
        this.startInAppPayWithCustomSheetSuccess = success;
        return this;
    }

    MockPaymentManagerBuilder startInAppPayWithCustomSheetError(int errorCode) {
        this.startInAppPayWithCustomSheetError = errorCode;
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

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                PaymentManager.CustomSheetTransactionInfoListener callback =
                    (PaymentManager.CustomSheetTransactionInfoListener) invocation.getArguments()[1];

                if (cardInfo != null && customSheet != null) {
                    callback.onCardInfoUpdated(cardInfo, customSheet);
                } else if (startInAppPayWithCustomSheetPaymentInfo != null && startInAppPayWithCustomSheetSuccess != null) {
                    callback.onSuccess(startInAppPayWithCustomSheetPaymentInfo, startInAppPayWithCustomSheetSuccess, new Bundle());
                } else if (startInAppPayWithCustomSheetError != null) {
                    callback.onFailure(startInAppPayWithCustomSheetError, new Bundle());
                }
                return null;
            }
        }).when(paymentManager).startInAppPayWithCustomSheet(any(CustomSheetPaymentInfo.class), any (PaymentManager.CustomSheetTransactionInfoListener.class));

        return paymentManager;
    }
}
