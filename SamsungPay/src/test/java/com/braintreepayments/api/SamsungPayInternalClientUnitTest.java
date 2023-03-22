package com.braintreepayments.api;

import android.os.Bundle;

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
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.ERROR_SPAY_APP_NEED_TO_UPDATE;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.ERROR_SPAY_SETUP_NOT_COMPLETED;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.EXTRA_ERROR_REASON;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_READY;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_SUPPORTED;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_READY;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class SamsungPayInternalClientUnitTest {

    private Configuration configuration;

    @Captor
    private ArgumentCaptor<List<SpaySdk.Brand>> cardBrandsCaptor;

    @Before
    public void beforeEach() throws JSONException {
        MockitoAnnotations.initMocks(this);
        configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY);
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
    public void getSamsungPayStatus_whenSamsungPayIsReady_forwardsSuccessResultFromSamsungPay() {
        PaymentManager paymentManager = mock(PaymentManager.class);
        SamsungPay samsungPay = new MockSamsungPayBuilder()
                .successStatusCode(SPAY_READY)
                .build();
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        GetSamsungPayStatusCallback callback = mock(GetSamsungPayStatusCallback.class);
        sut.getSamsungPayStatus(callback);

        verify(callback).onResult(SPAY_READY, null);
    }

    @Test
    public void getSamsungPayStatus_whenSamsungPayIsNotReady_forwardsNotReadyErrorByDefault() {
        PaymentManager paymentManager = mock(PaymentManager.class);
        SamsungPay samsungPay = new MockSamsungPayBuilder()
                .successStatusCode(SPAY_NOT_READY)
                .build();
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        GetSamsungPayStatusCallback callback = mock(GetSamsungPayStatusCallback.class);
        sut.getSamsungPayStatus(callback);

        ArgumentCaptor<SamsungPayException> captor =
                ArgumentCaptor.forClass(SamsungPayException.class);
        verify(callback).onResult(eq(SPAY_NOT_READY), captor.capture());

        SamsungPayException exception = captor.getValue();
        assertEquals(SamsungPayError.SAMSUNG_PAY_NOT_READY, exception.getErrorCode());
    }

    @Test
    public void getSamsungPayStatus_whenSamsungPayIsNotSupported_forwardsNotSupportedErrorByDefault() {
        PaymentManager paymentManager = mock(PaymentManager.class);
        SamsungPay samsungPay = new MockSamsungPayBuilder()
                .successStatusCode(SPAY_NOT_SUPPORTED)
                .build();
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        GetSamsungPayStatusCallback callback = mock(GetSamsungPayStatusCallback.class);
        sut.getSamsungPayStatus(callback);

        ArgumentCaptor<SamsungPayException> captor =
                ArgumentCaptor.forClass(SamsungPayException.class);
        verify(callback).onResult(eq(SPAY_NOT_SUPPORTED), captor.capture());

        SamsungPayException exception = captor.getValue();
        assertEquals(SamsungPayError.SAMSUNG_PAY_NOT_SUPPORTED, exception.getErrorCode());
    }

    @Test
    public void getSamsungPayStatus_whenSamsungPayIsNotReady_andSamsungPayAppNeedsUpdate_forwardsAppNeedsUpdateError() {
        Bundle errorBundle = new Bundle();
        errorBundle.putInt(EXTRA_ERROR_REASON, ERROR_SPAY_APP_NEED_TO_UPDATE);

        PaymentManager paymentManager = mock(PaymentManager.class);
        SamsungPay samsungPay = new MockSamsungPayBuilder()
                .successStatusCode(SPAY_NOT_READY)
                .successBundle(errorBundle)
                .build();
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        GetSamsungPayStatusCallback callback = mock(GetSamsungPayStatusCallback.class);
        sut.getSamsungPayStatus(callback);

        ArgumentCaptor<SamsungPayException> captor =
                ArgumentCaptor.forClass(SamsungPayException.class);
        verify(callback).onResult(eq(SPAY_NOT_READY), captor.capture());

        SamsungPayException exception = captor.getValue();
        assertEquals(SamsungPayError.SAMSUNG_PAY_APP_NEEDS_UPDATE, exception.getErrorCode());
    }

    @Test
    public void getSamsungPayStatus_whenSamsungPayIsNotReady_andSamsungPaySetupNotComplete_forwardsSetupNotCompleteError() {
        Bundle errorBundle = new Bundle();
        errorBundle.putInt(EXTRA_ERROR_REASON, ERROR_SPAY_SETUP_NOT_COMPLETED);

        PaymentManager paymentManager = mock(PaymentManager.class);
        SamsungPay samsungPay = new MockSamsungPayBuilder()
                .successStatusCode(SPAY_NOT_READY)
                .successBundle(errorBundle)
                .build();
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        GetSamsungPayStatusCallback callback = mock(GetSamsungPayStatusCallback.class);
        sut.getSamsungPayStatus(callback);

        ArgumentCaptor<SamsungPayException> captor =
                ArgumentCaptor.forClass(SamsungPayException.class);
        verify(callback).onResult(eq(SPAY_NOT_READY), captor.capture());

        SamsungPayException exception = captor.getValue();
        assertEquals(SamsungPayError.SAMSUNG_PAY_SETUP_NOT_COMPLETED, exception.getErrorCode());
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

        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        GetAcceptedCardBrandsCallback callback = mock(GetAcceptedCardBrandsCallback.class);
        sut.getAcceptedCardBrands(callback);
        verify(callback).onResult(cardBrandsCaptor.capture(), (Exception) isNull());

        List<SpaySdk.Brand> acceptedCardBrands = cardBrandsCaptor.getValue();
        assertEquals(2, acceptedCardBrands.size());

        Set<SpaySdk.Brand> expected = new HashSet<>(
                Arrays.asList(SpaySdk.Brand.MASTERCARD, SpaySdk.Brand.VISA));
        Set<SpaySdk.Brand> actual = new HashSet<>(acceptedCardBrands);
        assertEquals(expected, actual);
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
        SamsungPayListener listener = mock(SamsungPayListener.class);
        sut.startSamsungPay(customSheetPaymentInfo, listener);

        verify(paymentManager).updateSheet(customSheet);
        verify(listener).onSamsungPayCardInfoUpdated(cardInfo, customSheet);
    }

    @Test
    public void startSamsungPay_onSuccess_notifiesListenerOfNonceCreation() {
        CustomSheetPaymentInfo customSheetPaymentInfo = mock(CustomSheetPaymentInfo.class);
        PaymentManager paymentManager = new MockPaymentManagerBuilder()
                .startInAppPayWithCustomSheetSuccess(customSheetPaymentInfo, Fixtures.SAMSUNG_PAY_RESPONSE_V2)
                .build();

        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        SamsungPayListener listener = mock(SamsungPayListener.class);
        sut.startSamsungPay(customSheetPaymentInfo, listener);

        verify(listener).onSamsungPayStartSuccess(any(SamsungPayNonce.class), same(customSheetPaymentInfo));
    }

    @Test
    public void startSamsungPay_onSuccess_whenJSONInvalid_notifiesListenerOfError() {
        CustomSheetPaymentInfo customSheetPaymentInfo = mock(CustomSheetPaymentInfo.class);
        PaymentManager paymentManager = new MockPaymentManagerBuilder()
                .startInAppPayWithCustomSheetSuccess(customSheetPaymentInfo, "invalid json")
                .build();

        SamsungPay samsungPay = mock(SamsungPay.class);
        SamsungPayInternalClient sut = new SamsungPayInternalClient(configuration, samsungPay, paymentManager);

        SamsungPayListener listener = mock(SamsungPayListener.class);
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

        SamsungPayListener listener = mock(SamsungPayListener.class);
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

        SamsungPayListener listener = mock(SamsungPayListener.class);
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

        SamsungPayListener listener = mock(SamsungPayListener.class);
        sut.startSamsungPay(customSheetPaymentInfo, listener);

        ArgumentCaptor<UserCanceledException> captor = ArgumentCaptor.forClass(UserCanceledException.class);
        verify(listener).onSamsungPayStartError(captor.capture());

        UserCanceledException error = captor.getValue();
        assertEquals("User canceled Samsung Pay.", error.getMessage());
        assertTrue(error.isExplicitCancelation());
    }
}