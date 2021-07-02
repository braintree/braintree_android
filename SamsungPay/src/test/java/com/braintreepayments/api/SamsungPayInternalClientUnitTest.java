package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.samsung.android.sdk.samsungpay.v2.SamsungPay;
import com.samsung.android.sdk.samsungpay.v2.SpaySdk;
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SamsungPayInternalClientUnitTest {

    private Configuration configuration;

    @Captor
    private ArgumentCaptor<List<SpaySdk.Brand>> cardBrandsCaptor;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        configuration = mock(Configuration.class);
    }

    @Test
    public void goToUpdatePage_forwardsInvocationToSamsungPay() {
        PaymentManager paymentManager = mock(PaymentManager.class);
        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        sut.goToSamsungPayUpdatePage();
        verify(samsungPay).goToUpdatePage();
    }

    @Test
    public void activateSamsungPay_forwardsInvocationToSamsungPay() {
        PaymentManager paymentManager = mock(PaymentManager.class);
        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        sut.activateSamsungPay();
        verify(samsungPay).activateSamsungPay();
    }

    @Test
    public void getSamsungPayStatus_forwardsSuccessResultFromSamsungPay() {
        PaymentManager paymentManager = mock(PaymentManager.class);
        SamsungPay samsungPay = new MockSamsungPayBuilder()
                .successStatusCode(123)
                .build();

        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

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

        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        GetSamsungPayStatusCallback callback = mock(GetSamsungPayStatusCallback.class);
        sut.getSamsungPayStatus(callback);

        ArgumentCaptor<SamsungPayException> captor =
                ArgumentCaptor.forClass(SamsungPayException.class);
        verify(callback).onResult((Integer) isNull(), captor.capture());

        SamsungPayException exception = captor.getValue();
        assertEquals(456, exception.getErrorCode());
    }

    @Test
    public void getAcceptedCardBrands_returnsCardsSupportedByBothBraintreeConfigAndSamsungPay() {
        CardInfo visaCardInfo = mock(CardInfo.class);
        when(visaCardInfo.getBrand()).thenReturn(SpaySdk.Brand.VISA);

        CardInfo masterCardInfo = mock(CardInfo.class);
        when(masterCardInfo.getBrand()).thenReturn(SpaySdk.Brand.MASTERCARD);

        PaymentManager paymentManager = new MockPaymentManagerBuilder()
                .requestCardInfoSuccess(Arrays.asList(visaCardInfo, masterCardInfo))
                .build();

        when(configuration.getSupportedCardTypes()).thenReturn(Arrays.asList("mastercard", "american_express"));

        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        GetAcceptedCardBrandsCallback callback = mock(GetAcceptedCardBrandsCallback.class);
        sut.getAcceptedCardBrands(callback);
        verify(callback).onResult(cardBrandsCaptor.capture(), (Exception) isNull());

        List<SpaySdk.Brand> acceptedCardBrands = cardBrandsCaptor.getValue();
        assertEquals(1, acceptedCardBrands.size());
        assertEquals(SpaySdk.Brand.MASTERCARD, acceptedCardBrands.get(0));
    }

    @Test
    public void getAcceptedCardBrands_forwardsErrorResultFromPaymentManager() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        PaymentManager paymentManager = new MockPaymentManagerBuilder()
                .requestCardInfoErrorCode(456)
                .build();

        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        // NOTE: got an error when trying to get this test to run with an ArgumentCaptor
        sut.getAcceptedCardBrands(new GetAcceptedCardBrandsCallback() {
            @Override
            public void onResult(@Nullable List<SpaySdk.Brand> acceptedCardBrands, @Nullable Exception error) {
                assertNotNull(error);

                SamsungPayException samsungPayException = (SamsungPayException) error;
                assertEquals(456, samsungPayException.getErrorCode());
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
    }

    @Test
    public void startSamsungPay_onCardInfoUpdated_updatesPaymentManagerSheetAndNotifiesListener() {
        CardInfo cardInfo = mock(CardInfo.class);
        CustomSheet customSheet = mock(CustomSheet.class);
        PaymentManager paymentManager = new MockPaymentManagerBuilder()
                .startInAppPayWithCustomSheetCardInfoUpdated(cardInfo, customSheet)
                .build();

        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        CustomSheetPaymentInfo customSheetPaymentInfo = mock(CustomSheetPaymentInfo.class);
        SamsungPayStartListener listener = mock(SamsungPayStartListener.class);
        sut.startSamsungPay(customSheetPaymentInfo, listener);

        verify(paymentManager).updateSheet(customSheet);
        verify(listener).onSamsungPayCardInfoUpdated(cardInfo, customSheet);
    }

    @Test
    public void startSamsungPay_onSuccess_notifiesListenerOfNonceCreation() {
        CustomSheetPaymentInfo customSheetPaymentInfo = mock(CustomSheetPaymentInfo.class);
        PaymentManager paymentManager = new MockPaymentManagerBuilder()
                .startInAppPayWithCustomSheetSuccess(customSheetPaymentInfo, Fixtures.SAMSUNG_PAY_RESPONSE)
                .build();

        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        SamsungPayStartListener listener = mock(SamsungPayStartListener.class);
        sut.startSamsungPay(customSheetPaymentInfo, listener);

        verify(listener).onSamsungPayStartSuccess(any(SamsungPayNonce.class));
    }

    @Test
    public void startSamsungPay_onSuccess_whenJSONInvalid_notifiesListenerOfError() {
        CustomSheetPaymentInfo customSheetPaymentInfo = mock(CustomSheetPaymentInfo.class);
        PaymentManager paymentManager = new MockPaymentManagerBuilder()
                .startInAppPayWithCustomSheetSuccess(customSheetPaymentInfo, "invalid json")
                .build();

        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        SamsungPayStartListener listener = mock(SamsungPayStartListener.class);
        sut.startSamsungPay(customSheetPaymentInfo, listener);

        verify(listener).onSamsungPayStartError(any(JSONException.class));
    }

    @Test
    public void startSamsungPay_onSuccess_whenJSONIsNotValidSamsungPayResponse_notifiesListenerOfError() {
        CustomSheetPaymentInfo customSheetPaymentInfo = mock(CustomSheetPaymentInfo.class);
        PaymentManager paymentManager = new MockPaymentManagerBuilder()
                .startInAppPayWithCustomSheetSuccess(customSheetPaymentInfo, "{}")
                .build();

        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        SamsungPayStartListener listener = mock(SamsungPayStartListener.class);
        sut.startSamsungPay(customSheetPaymentInfo, listener);

        verify(listener).onSamsungPayStartError(any(JSONException.class));
    }

    @Test
    public void startSamsungPay_onError_forwardsErrorCodeByDefault() {
        CustomSheetPaymentInfo customSheetPaymentInfo = mock(CustomSheetPaymentInfo.class);
        PaymentManager paymentManager = new MockPaymentManagerBuilder()
                .startInAppPayWithCustomSheetError(123)
                .build();

        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        SamsungPayStartListener listener = mock(SamsungPayStartListener.class);
        sut.startSamsungPay(customSheetPaymentInfo, listener);

        ArgumentCaptor<SamsungPayException> captor = ArgumentCaptor.forClass(SamsungPayException.class);
        verify(listener).onSamsungPayStartError(captor.capture());

        SamsungPayException error = captor.getValue();
        assertEquals(123, error.getErrorCode());
    }

    @Test
    public void startSamsungPay_onError_whenUserCancelled_notifiesListerOfUserCancelation() {
        CustomSheetPaymentInfo customSheetPaymentInfo = mock(CustomSheetPaymentInfo.class);
        PaymentManager paymentManager = new MockPaymentManagerBuilder()
                .startInAppPayWithCustomSheetError(SpaySdk.ERROR_USER_CANCELED)
                .build();

        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        SamsungPayStartListener listener = mock(SamsungPayStartListener.class);
        sut.startSamsungPay(customSheetPaymentInfo, listener);

        ArgumentCaptor<UserCanceledException> captor = ArgumentCaptor.forClass(UserCanceledException.class);
        verify(listener).onSamsungPayStartError(captor.capture());

        UserCanceledException error = captor.getValue();
        assertEquals("User Canceled", error.getMessage());
    }
}