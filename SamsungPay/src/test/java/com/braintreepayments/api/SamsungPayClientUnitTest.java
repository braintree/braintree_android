package com.braintreepayments.api;

import com.samsung.android.sdk.samsungpay.v2.SpaySdk;

import org.junit.Test;

import java.util.Collections;

import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_READY;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_SUPPORTED;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_READY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SamsungPayClientUnitTest {

    @Test
    public void goToUpdatePage_forwardsInvocationToInternalClient() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        SamsungPayInternalClient internalClient = mock(SamsungPayInternalClient.class);
        sut.internalClient = internalClient;

        sut.goToUpdatePage();
        verify(internalClient).goToSamsungPayUpdatePage();
    }

    @Test
    public void activateSamsungPay_forwardsInvocationToInternalClient() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        SamsungPayInternalClient internalClient = mock(SamsungPayInternalClient.class);
        sut.internalClient = internalClient;

        sut.activateSamsungPay();
        verify(internalClient).activateSamsungPay();
    }

    @Test
    public void isReadyToPay_whenSamsungPayStatusIsNotReady_callsBackFalse() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_NOT_READY)
                .build();

        SamsungIsReadyToPayCallback callback = mock(SamsungIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(callback).onResult(false, null);
    }

    @Test
    public void isReadyToPay_whenSamsungPayStatusIsNotSupported_callsBackFalse() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_NOT_SUPPORTED)
                .build();

        SamsungIsReadyToPayCallback callback = mock(SamsungIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(callback).onResult(false, null);
    }

    @Test
    public void isReadyToPay_whenSamsungPayStatusErrorOccurs_callsBackFalseAndPropagatesError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        SamsungPayException error = new SamsungPayException(123);
        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusError(error)
                .build();

        SamsungIsReadyToPayCallback callback = mock(SamsungIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(callback).onResult(false, error);
    }

    @Test
    public void isReadyToPay_whenSamsungPayStatusIsReady_andAcceptedCardsExist_callsBackTrue() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_READY)
                .getAcceptedCardBrandsSuccess(Collections.singletonList(SpaySdk.Brand.VISA))
                .build();

        SamsungIsReadyToPayCallback callback = mock(SamsungIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(callback).onResult(true, null);
    }

    @Test
    public void isReadyToPay_whenSamsungPayReady_andNoAcceptedCardsExist_callsBackFalse() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_READY)
                .getAcceptedCardBrandsSuccess(Collections.<SpaySdk.Brand>emptyList())
                .build();

        SamsungIsReadyToPayCallback callback = mock(SamsungIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(callback).onResult(false, null);
    }

    @Test
    public void isReadyToPay_whenGetAcceptedCardBrandsErrorOccurs_callsBackFalseAndPropagatesError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        SamsungPayException error = new SamsungPayException(123);
        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_READY)
                .getAcceptedCardBrandsError(error)
                .build();

        SamsungIsReadyToPayCallback callback = mock(SamsungIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(callback).onResult(false, error);
    }
}