package com.braintreepayments.api;

import android.content.Context;

import com.samsung.android.sdk.samsungpay.v2.SpaySdk;
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;

import org.json.JSONException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_READY;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_SUPPORTED;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_READY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SamsungPayClientUnitTest {

    @Test
    public void getInternalClient_lazilyCreatesSamsungPayInternalClientFromBraintreeConfiguration() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY))
                .build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        Context context = mock(Context.class);
        when(braintreeClient.getApplicationContext()).thenReturn(context);

        GetSamsungPayInternalClientCallback callback0 = mock(GetSamsungPayInternalClientCallback.class);
        GetSamsungPayInternalClientCallback callback1 = mock(GetSamsungPayInternalClientCallback.class);

        sut.getInternalClient(callback0);
        sut.getInternalClient(callback1);

        ArgumentCaptor<SamsungPayInternalClient> captor0 =
                ArgumentCaptor.forClass(SamsungPayInternalClient.class);
        verify(callback0).onResult(captor0.capture(), (Exception) isNull());

        ArgumentCaptor<SamsungPayInternalClient> captor1 =
            ArgumentCaptor.forClass(SamsungPayInternalClient.class);
        verify(callback1).onResult(captor1.capture(), (Exception) isNull());

        assertSame(captor0.getValue(), captor1.getValue());
    }

    @Test
    public void getInternalClient_sendsAnalyticsEventOnInitialInstantiation() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY))
                .build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        Context context = mock(Context.class);
        when(braintreeClient.getApplicationContext()).thenReturn(context);

        GetSamsungPayInternalClientCallback callback0 =
            mock(GetSamsungPayInternalClientCallback.class);
        GetSamsungPayInternalClientCallback callback1 =
            mock(GetSamsungPayInternalClientCallback.class);

        sut.getInternalClient(callback0);
        sut.getInternalClient(callback1);

        verify(braintreeClient, times(1))
                .sendAnalyticsEvent("samsung-pay.create-payment-manager.success");
    }

    @Test
    public void goToUpdatePage_forwardsInvocationToInternalClient_andNotifiesCompletion() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        SamsungPayInternalClient internalClient = mock(SamsungPayInternalClient.class);
        sut.internalClient = internalClient;

        SamsungPayUpdateCallback callback = mock(SamsungPayUpdateCallback.class);
        sut.updateSamsungPay(callback);

        verify(internalClient).goToSamsungPayUpdatePage();
        verify(callback).onResult(null);
    }

    @Test
    public void activateSamsungPay_forwardsInvocationToInternalClient_andNotifiesCompletion() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        SamsungPayInternalClient internalClient = mock(SamsungPayInternalClient.class);
        sut.internalClient = internalClient;

        SamsungPayActivateCallback callback = mock(SamsungPayActivateCallback.class);
        sut.activateSamsungPay(callback);

        verify(internalClient).activateSamsungPay();
        verify(callback).onResult(null);
    }

    @Test
    public void isReadyToPay_whenSamsungPayStatusIsNotReady_callsBackFalseAndForwardsError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        Exception samsungPayError = new Exception("samsung error");
        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_NOT_READY)
                .getSamsungPayStatusError(samsungPayError)
                .build();

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(callback).onResult(false, samsungPayError);
    }

    @Test
    public void isReadyToPay_whenSamsungPayStatusIsNotReady_sendsAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_NOT_READY)
                .build();

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(braintreeClient).sendAnalyticsEvent("samsung-pay.is-ready-to-pay.not-ready");
    }

    @Test
    public void isReadyToPay_whenSamsungPayStatusIsNotSupported_callsBackFalseAndForwardsError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        Exception samsungPayError = new Exception("samsung error");
        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_NOT_SUPPORTED)
                .getSamsungPayStatusError(samsungPayError)
                .build();

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(callback).onResult(false, samsungPayError);
    }

    @Test
    public void isReadyToPay_whenSamsungPayStatusIsNotSupported_sendsAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_NOT_SUPPORTED)
                .build();

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(braintreeClient).sendAnalyticsEvent("samsung-pay.is-ready-to-pay.device-not-supported");
    }

    @Test
    public void isReadyToPay_whenSamsungPayStatusErrorOccurs_callsBackFalseAndPropagatesError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        SamsungPayException error = new SamsungPayException(123);
        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusError(error)
                .build();

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
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

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(callback).onResult(true, null);
    }

    @Test
    public void isReadyToPay_whenSamsungPayStatusIsReady_andAcceptedCardsExist_sendsAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_READY)
                .getAcceptedCardBrandsSuccess(Collections.singletonList(SpaySdk.Brand.VISA))
                .build();

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(braintreeClient).sendAnalyticsEvent("samsung-pay.is-ready-to-pay.ready");
    }

    @Test
    public void isReadyToPay_whenSamsungPayStatusIsReady_andNoAcceptedCardsExist_callsBackFalseWithError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_READY)
                .getAcceptedCardBrandsSuccess(Collections.<SpaySdk.Brand>emptyList())
                .build();

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        ArgumentCaptor<SamsungPayException> captor = ArgumentCaptor.forClass(SamsungPayException.class);
        verify(callback).onResult(eq(false), captor.capture());

        SamsungPayException error = captor.getValue();
        assertEquals(SamsungPayError.SAMSUNG_PAY_NO_SUPPORTED_CARDS_IN_WALLET, error.getErrorCode());
    }

    @Test
    public void isReadyToPay_whenSamsungPayStatusIsReady_andNoAcceptedCardsExist_sendsAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_READY)
                .getAcceptedCardBrandsSuccess(Collections.<SpaySdk.Brand>emptyList())
                .build();

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(braintreeClient).sendAnalyticsEvent("samsung-pay.request-card-info.no-supported-cards-in-wallet");
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

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
        sut.isReadyToPay(callback);

        verify(callback).onResult(false, error);
    }

    @Test
    public void startSamsungPay_forwardsInvocationToInternalClient() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        SamsungPayInternalClient internalClient = mock(SamsungPayInternalClient.class);
        sut.internalClient = internalClient;

        CustomSheetPaymentInfo paymentInfo = mock(CustomSheetPaymentInfo.class);
        SamsungPayListener listener = mock(SamsungPayListener.class);
        sut.startSamsungPay(paymentInfo, listener);

        verify(internalClient).startSamsungPay(paymentInfo, listener);
    }

    @Test
    public void buildCustomSheetPaymentInfo_callsBackBuilderPreConfiguredWithBraintreeAttributes() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY))
                .build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        BuildCustomSheetPaymentInfoCallback callback =
                mock(BuildCustomSheetPaymentInfoCallback.class);
        sut.buildCustomSheetPaymentInfo(callback);

        ArgumentCaptor<CustomSheetPaymentInfo.Builder> captor =
                ArgumentCaptor.forClass(CustomSheetPaymentInfo.Builder.class);
        verify(callback).onResult(captor.capture(), (Exception) isNull());

        CustomSheetPaymentInfo.Builder builder = captor.getValue();
        CustomSheetPaymentInfo paymentInfo = builder.build();

        assertEquals("some example merchant", paymentInfo.getMerchantName());
        assertEquals("example-samsung-authorization", paymentInfo.getMerchantId());

        Set<SpaySdk.Brand> expectedCardBrands = new HashSet<>(Arrays.asList(
                SpaySdk.Brand.AMERICANEXPRESS,
                SpaySdk.Brand.DISCOVER,
                SpaySdk.Brand.MASTERCARD,
                SpaySdk.Brand.VISA
        ));
        assertEquals(expectedCardBrands, new HashSet<>(paymentInfo.getAllowedCardBrands()));
    }

    @Test
    public void buildCustomSheetPaymentInfo_sendsAnalyticsEvent() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY))
                .build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        BuildCustomSheetPaymentInfoCallback callback =
                mock(BuildCustomSheetPaymentInfoCallback.class);
        sut.buildCustomSheetPaymentInfo(callback);

        verify(braintreeClient).sendAnalyticsEvent("samsung-pay.create-payment-info.success");
    }

    @Test
    public void buildCustomSheetPaymentInfo_forwardsConfigurationErrors() {
        Exception configError = new Exception("config error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        BuildCustomSheetPaymentInfoCallback callback =
                mock(BuildCustomSheetPaymentInfoCallback.class);
        sut.buildCustomSheetPaymentInfo(callback);

        verify(callback).onResult((CustomSheetPaymentInfo.Builder) isNull(), same(configError));
    }
}