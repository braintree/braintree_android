package com.braintreepayments.api;

import com.samsung.android.sdk.samsungpay.v2.SamsungPay;
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SamsungPayInternalClientTest {

    @Test
    public void getSamsungPayStatus_forwardsSuccessResultFromSamsungPay() {
        PaymentManager paymentManager = mock(PaymentManager.class);
        SamsungPay samsungPay = new MockSamsungPayBuilder()
                .successStatusCode(123)
                .build();

        SamsungPayInternalClient sut = new SamsungPayInternalClient(samsungPay, paymentManager);

        GetSamsungPayStatusCallback callback = mock(GetSamsungPayStatusCallback.class);
        sut.getSamsungPayStatus(callback);

        verify(callback).onResult(123, null);
    }
    
    @Test
    public void getSamsungPayStatus_forwardsErrorResultFromSamsungPay() {
        PaymentManager paymentManager = mock(PaymentManager.class);
        SamsungPay samsungPay = new MockSamsungPayBuilder()
                .errorCode(456)
                .build();

        SamsungPayInternalClient sut = new SamsungPayInternalClient(samsungPay, paymentManager);

        GetSamsungPayStatusCallback callback = mock(GetSamsungPayStatusCallback.class);
        sut.getSamsungPayStatus(callback);

        ArgumentCaptor<SamsungPayException> captor =
            ArgumentCaptor.forClass(SamsungPayException.class);
        verify(callback).onResult((Integer) isNull(), captor.capture());

        SamsungPayException exception = captor.getValue();
        assertEquals(456, exception.getErrorCode());
    }
}