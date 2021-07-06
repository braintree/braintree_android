package com.braintreepayments.api;

import com.samsung.android.sdk.samsungpay.v2.SpaySdk;
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;

import org.json.JSONException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_READY;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_SUPPORTED;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_READY;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
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

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
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

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
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
    public void isReadyToPay_whenSamsungPayReady_andNoAcceptedCardsExist_callsBackFalse() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        SamsungPayClient sut = new SamsungPayClient(braintreeClient);

        sut.internalClient = new MockSamsungPayInternalClientBuilder()
                .getSamsungPayStatusSuccess(SPAY_READY)
                .getAcceptedCardBrandsSuccess(Collections.<SpaySdk.Brand>emptyList())
                .build();

        SamsungPayIsReadyToPayCallback callback = mock(SamsungPayIsReadyToPayCallback.class);
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
        SamsungPayStartListener listener = mock(SamsungPayStartListener.class);
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

        List<SpaySdk.Brand> expectedCardBrands = Arrays.asList(
                SpaySdk.Brand.DISCOVER,
                SpaySdk.Brand.MASTERCARD,
                SpaySdk.Brand.VISA,
                SpaySdk.Brand.AMERICANEXPRESS
        );
        assertEquals(expectedCardBrands, paymentInfo.getAllowedCardBrands());
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