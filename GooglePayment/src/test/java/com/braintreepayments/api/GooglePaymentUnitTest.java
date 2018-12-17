package com.braintreepayments.api;

import android.content.Intent;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.FixturesHelper;
import com.braintreepayments.api.test.TestConfigurationBuilder;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.GooglePaymentActivity.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePaymentActivity.EXTRA_PAYMENT_DATA_REQUEST;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GooglePaymentUnitTest {

    private GooglePaymentRequest mBaseRequest;

    @Before
    public void setup() {
       mBaseRequest = new GooglePaymentRequest()
            .transactionInfo(TransactionInfo.newBuilder()
                    .setTotalPrice("1.00")
                    .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                    .setCurrencyCode("USD")
                    .build());
    }

    @Test
    public void requestPayment_whenSandbox_setsTestEnvironment() throws JSONException {
        BraintreeFragment fragment = getSetupFragment("sandbox");

        GooglePayment.requestPayment(fragment, mBaseRequest);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        assertEquals("TEST", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void requestPayment_whenProduction_setsProductionEnvironment() throws JSONException {
        BraintreeFragment fragment = getSetupFragment("production");

        GooglePayment.requestPayment(fragment, mBaseRequest);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_PRODUCTION, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        assertEquals("PRODUCTION", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void requestPayment_withGoogleMerchantId_sendGoogleMerchantId() throws JSONException {
        BraintreeFragment fragment = getSetupFragment();
        GooglePaymentRequest googlePaymentRequest = mBaseRequest
                .googleMerchantId("google-merchant-id-override");

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals("google-merchant-id-override", paymentDataRequestJson
                .getJSONObject("merchantInfo")
                .getString("merchantId"));
    }

    @Test
    public void requestPayment_withGoogleMerchantName_sendGoogleMerchantName() throws JSONException {
        BraintreeFragment fragment = getSetupFragment();
        GooglePaymentRequest googlePaymentRequest = mBaseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals("google-merchant-name-override", paymentDataRequestJson
                .getJSONObject("merchantInfo")
                .getString("merchantName"));
    }

    @Test
    public void tokenize_withCardToken_returnsGooglePaymentNonce() {
        String paymentDataJson = FixturesHelper.stringFromFixture("response/google_payment/card.json");
        BraintreeFragment fragment = getSetupFragment();
        PaymentData pd = PaymentData.fromJson(paymentDataJson);

        GooglePayment.tokenize(fragment, pd);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(fragment).postCallback(captor.capture());

        assertTrue(captor.getValue() instanceof GooglePaymentCardNonce);
    }

    @Test
    public void tokenize_withPayPalToken_returnsPayPalAccountNonce() {
        String paymentDataJson = FixturesHelper.stringFromFixture("payment_methods/paypal_account_response.json");

        BraintreeFragment fragment = getSetupFragment();
        PaymentData pd = PaymentData.fromJson(paymentDataJson);

        GooglePayment.tokenize(fragment, pd);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(fragment).postCallback(captor.capture());

        assertTrue(captor.getValue() instanceof PayPalAccountNonce);
    }

    private BraintreeFragment getSetupFragment() {
        return getSetupFragment("sandbox");
    }

    private BraintreeFragment getSetupFragment(String environment) {
        String configuration = new TestConfigurationBuilder()
                .androidPay(new TestConfigurationBuilder.TestAndroidPayConfigurationBuilder()
                        .environment(environment)
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .paypal(new TestConfigurationBuilder.TestPayPalConfigurationBuilder(true)
                        .clientId("paypal-client-id"))
                .withAnalytics()
                .build();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        try {
            when(fragment.getAuthorization()).thenReturn(Authorization.fromString("sandbox_tokenization_key"));
        } catch (InvalidArgumentException e) {
            throw new RuntimeException(e);
        }

        return fragment;
    }
}
