package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.googlepayment.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collection;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.GooglePaymentClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePaymentClient.EXTRA_PAYMENT_DATA_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(RobolectricTestRunner.class)
@PrepareForTest({GoogleApiAvailability.class, Wallet.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
public class GooglePaymentClientUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private FragmentActivity activity;
    private BraintreeClient braintreeClient;

    private GooglePaymentRequest baseRequest;

    private GooglePaymentIsReadyToPayCallback readyToPayCallback;
    private GooglePaymentRequestPaymentCallback requestPaymentCallback;
    private GooglePaymentOnActivityResultCallback activityResultCallback;

    private ActivityInfo activityInfo;

    @Before
    public void beforeEach() {
        activity = mock(FragmentActivity.class);
        braintreeClient = mock(BraintreeClient.class);
        readyToPayCallback = mock(GooglePaymentIsReadyToPayCallback.class);
        requestPaymentCallback = mock(GooglePaymentRequestPaymentCallback.class);
        activityResultCallback = mock(GooglePaymentOnActivityResultCallback.class);
        activityInfo = mock(ActivityInfo.class);

        baseRequest = new GooglePaymentRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .supportedNetworks(new String[]{"amex", "visa"})
                        .enabled(true))
                .buildConfiguration();

        braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        GoogleApiAvailability mockGoogleApiAvailability = mock(GoogleApiAvailability.class);
        when(mockGoogleApiAvailability.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        mockStatic(GoogleApiAvailability.class);
        when(GoogleApiAvailability.getInstance()).thenReturn(mockGoogleApiAvailability);

        when(activityInfo.getThemeResource()).thenReturn(R.style.bt_transparent_activity);
    }

    // region isReadyToPay

    @Test
    public void isReadyToPay_sendsReadyToPayRequest() throws JSONException {
        PaymentsClient mockPaymentsClient = mock(PaymentsClient.class);
        when(mockPaymentsClient.isReadyToPay(any(IsReadyToPayRequest.class))).thenReturn(Tasks.forResult(true));

        mockStatic(Wallet.class);
        when(Wallet.getPaymentsClient(any(Activity.class), any(Wallet.WalletOptions.class))).thenReturn(mockPaymentsClient);

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);
        sut.isReadyToPay(activity, null, readyToPayCallback);

        ArgumentCaptor<IsReadyToPayRequest> captor = ArgumentCaptor.forClass(IsReadyToPayRequest.class);
        verify(mockPaymentsClient).isReadyToPay(captor.capture());
        String actualJson = captor.getValue().toJson();

        JSONAssert.assertEquals(
                Fixtures.READY_TO_PAY_REQUEST_WITHOUT_EXISTING_PAYMENT_METHOD, actualJson, false);
    }

    @Test
    public void isReadyToPay_whenExistingPaymentMethodRequired_sendsIsReadyToPayRequestWithExistingPaymentRequired() throws JSONException {
        PaymentsClient mockPaymentsClient = mock(PaymentsClient.class);
        when(mockPaymentsClient.isReadyToPay(any(IsReadyToPayRequest.class))).thenReturn(Tasks.forResult(true));

        mockStatic(Wallet.class);
        when(Wallet.getPaymentsClient(any(Activity.class), any(Wallet.WalletOptions.class))).thenReturn(mockPaymentsClient);

        ReadyForGooglePaymentRequest readyForGooglePaymentRequest = new ReadyForGooglePaymentRequest().existingPaymentMethodRequired(true);

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);
        sut.isReadyToPay(activity, readyForGooglePaymentRequest, readyToPayCallback);

        ArgumentCaptor<IsReadyToPayRequest> captor = ArgumentCaptor.forClass(IsReadyToPayRequest.class);
        verify(mockPaymentsClient).isReadyToPay(captor.capture());
        String actualJson = captor.getValue().toJson();

        JSONAssert.assertEquals(
                Fixtures.READY_TO_PAY_REQUEST_WITH_EXISTING_PAYMENT_METHOD, actualJson, false);
    }

    @Test
    public void isReadyToPay_returnsFalseWhenGooglePaymentIsNotEnabled() throws Exception {
        PaymentsClient mockPaymentsClient = mock(PaymentsClient.class);
        when(mockPaymentsClient.isReadyToPay(any(IsReadyToPayRequest.class))).thenReturn(Tasks.forResult(true));

        mockStatic(Wallet.class);
        when(Wallet.getPaymentsClient(any(Activity.class), any(Wallet.WalletOptions.class))).thenReturn(mockPaymentsClient);

        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder().enabled(false))
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.isReadyToPay(activity, null, readyToPayCallback);
        verify(readyToPayCallback).onResult(false, null);
    }

    @Test
    public void isReadyToPay_whenActivityIsNull_forwardsErrorToCallback() throws InvalidArgumentException {
        PaymentsClient mockPaymentsClient = mock(PaymentsClient.class);
        when(mockPaymentsClient.isReadyToPay(any(IsReadyToPayRequest.class))).thenReturn(Tasks.forResult(true));

        mockStatic(Wallet.class);
        when(Wallet.getPaymentsClient(any(Activity.class), any(Wallet.WalletOptions.class))).thenReturn(mockPaymentsClient);

        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder().enabled(true))
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.isReadyToPay(null, null, readyToPayCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(readyToPayCallback).onResult(eq(false), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof GoogleApiClientException);
        assertEquals(GoogleApiClientException.ErrorType.NotAttachedToActivity, ((GoogleApiClientException) exception).getErrorType());
        assertEquals(1, ((GoogleApiClientException) exception).getErrorCode());
    }

    // endregion

    // region requestPayment

    @Test
    public void requestPayment_startsActivity() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, baseRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));
    }

    @Test
    public void requestPayment_startsActivityWithOptionalValues() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                .allowPrepaidCards(true)
                .billingAddressFormat(1)
                .billingAddressRequired(true)
                .emailRequired(true)
                .phoneNumberRequired(true)
                .shippingAddressRequired(true)
                .shippingAddressRequirements(ShippingAddressRequirements.newBuilder().addAllowedCountryCode("USA").build())
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));
        Intent intent = captor.getValue();

        assertEquals(GooglePaymentActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);

        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(2, paymentDataRequestJson.get("apiVersion"));
        assertEquals(0, paymentDataRequestJson.get("apiVersionMinor"));

        assertEquals(true, paymentDataRequestJson.get("emailRequired"));
        assertEquals(true, paymentDataRequestJson.get("shippingAddressRequired"));

        JSONObject transactionInfoJson = paymentDataRequestJson.getJSONObject("transactionInfo");
        assertEquals("FINAL", transactionInfoJson.getString("totalPriceStatus"));
        assertEquals("1.00", transactionInfoJson.getString("totalPrice"));
        assertEquals("USD", transactionInfoJson.getString("currencyCode"));

        JSONArray allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods");
        JSONObject paypal = allowedPaymentMethods.getJSONObject(0);
        assertEquals("PAYPAL", paypal.getString("type"));

        JSONArray purchaseUnits = paypal.getJSONObject("parameters")
                .getJSONObject("purchase_context")
                .getJSONArray("purchase_units");
        assertEquals(1, purchaseUnits.length());

        JSONObject purchaseUnit = purchaseUnits.getJSONObject(0);
        assertEquals("paypal-client-id-for-google-payment", purchaseUnit.getJSONObject("payee")
                .getString("client_id"));
        assertEquals("true", purchaseUnit.getString("recurring_payment"));

        JSONObject paypalTokenizationSpecification = paypal.getJSONObject("tokenizationSpecification");
        assertEquals("PAYMENT_GATEWAY", paypalTokenizationSpecification.getString("type"));

        JSONObject paypalTokenizationSpecificationParams = paypalTokenizationSpecification.getJSONObject("parameters");
        assertEquals("braintree", paypalTokenizationSpecificationParams.getString("gateway"));
        assertEquals("v1", paypalTokenizationSpecificationParams.getString("braintree:apiVersion"));

        String googlePaymentModuleVersion = com.braintreepayments.api.googlepayment.BuildConfig.VERSION_NAME;
        assertEquals(googlePaymentModuleVersion, paypalTokenizationSpecificationParams.getString("braintree:sdkVersion"));
        assertEquals("integration_merchant_id", paypalTokenizationSpecificationParams.getString("braintree:merchantId"));
        assertEquals("{\"source\":\"client\",\"version\":\"" + googlePaymentModuleVersion + "\",\"platform\":\"android\"}", paypalTokenizationSpecificationParams.getString("braintree:metadata"));
        assertFalse(paypalTokenizationSpecificationParams.has("braintree:clientKey"));
        assertEquals("paypal-client-id-for-google-payment", paypalTokenizationSpecificationParams.getString("braintree:paypalClientId"));

        JSONObject card = allowedPaymentMethods.getJSONObject(1);
        assertEquals("CARD", card.getString("type"));

        JSONObject cardParams = card.getJSONObject("parameters");
        assertTrue(cardParams.getBoolean("billingAddressRequired"));
        assertTrue(cardParams.getBoolean("allowPrepaidCards"));

        assertEquals("PAN_ONLY", cardParams.getJSONArray("allowedAuthMethods").getString(0));
        assertEquals("CRYPTOGRAM_3DS", cardParams.getJSONArray("allowedAuthMethods").getString(1));

        assertEquals("VISA", cardParams.getJSONArray("allowedCardNetworks").getString(0));
        assertEquals("MASTERCARD", cardParams.getJSONArray("allowedCardNetworks").getString(1));
        assertEquals("AMEX", cardParams.getJSONArray("allowedCardNetworks").getString(2));
        assertEquals("DISCOVER", cardParams.getJSONArray("allowedCardNetworks").getString(3));

        JSONObject tokenizationSpecification = card.getJSONObject("tokenizationSpecification");
        assertEquals("PAYMENT_GATEWAY", tokenizationSpecification.getString("type"));

        JSONObject cardTokenizationSpecificationParams = tokenizationSpecification.getJSONObject("parameters");
        assertEquals("braintree", cardTokenizationSpecificationParams.getString("gateway"));
        assertEquals("v1", cardTokenizationSpecificationParams.getString("braintree:apiVersion"));
        assertEquals(googlePaymentModuleVersion, cardTokenizationSpecificationParams.getString("braintree:sdkVersion"));
        assertEquals("integration_merchant_id", cardTokenizationSpecificationParams.getString("braintree:merchantId"));
        assertEquals("{\"source\":\"client\",\"version\":\"" + googlePaymentModuleVersion + "\",\"platform\":\"android\"}", cardTokenizationSpecificationParams.getString("braintree:metadata"));
        assertEquals("sandbox_tokenization_string", cardTokenizationSpecificationParams.getString("braintree:clientKey"));
    }

    @Test
    public void requestPayment_includesATokenizationKeyWhenPresent() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));
        Intent intent = captor.getValue();

        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods");
        JSONObject card = allowedPaymentMethods.getJSONObject(0);
        JSONObject tokenizationSpecification = card.getJSONObject("tokenizationSpecification");
        JSONObject cardTokenizationSpecificationParams = tokenizationSpecification.getJSONObject("parameters");
        assertEquals(Fixtures.TOKENIZATION_KEY, cardTokenizationSpecificationParams.get("braintree:clientKey"));
    }

    @Test
    public void requestPayment_doesNotIncludeATokenizationKeyWhenNotPresent() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));
        Intent intent = captor.getValue();

        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods");
        JSONObject card = allowedPaymentMethods.getJSONObject(0);
        JSONObject tokenizationSpecification = card.getJSONObject("tokenizationSpecification");
        JSONObject cardTokenizationSpecificationParams = tokenizationSpecification.getJSONObject("parameters");
        assertFalse(cardTokenizationSpecificationParams.has("braintree:clientKey"));
    }

    @Test
    public void requestPayment_sendsAnalyticsEvent() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        InOrder order = inOrder(braintreeClient);
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.selected"));
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.started"));
    }

    @Test
    public void requestPayment_postsExceptionWhenTransactionInfoIsNull() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        InOrder order = inOrder(braintreeClient);
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.selected"));
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));
    }

    @Test
    public void requestPayment_whenMerchantNotConfigured_returnsExceptionToFragment() {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);
        sut.requestPayment(activity, baseRequest, requestPaymentCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(requestPaymentCallback).onResult(eq(false), captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("Google Pay is not enabled for your Braintree account, or Google Play Services are not configured correctly.",
                captor.getValue().getMessage());
    }

    @Test
    public void requestPayment_whenSandbox_setsTestEnvironment() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, baseRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        assertEquals("TEST", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void requestPayment_whenProduction_setsProductionEnvironment() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("production")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, baseRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_PRODUCTION, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        assertEquals("PRODUCTION", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void requestPayment_withGoogleMerchantId_sendGoogleMerchantId() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantId("google-merchant-id-override");

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals("google-merchant-id-override", paymentDataRequestJson
                .getJSONObject("merchantInfo")
                .getString("merchantId"));
    }

    @Test
    public void requestPayment_withGoogleMerchantName_sendGoogleMerchantName() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals("google-merchant-name-override", paymentDataRequestJson
                .getJSONObject("merchantInfo")
                .getString("merchantName"));
    }

    @Test
    public void requestPayment_whenGooglePayCanProcessPayPal_tokenizationPropertiesIncludePayPal() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(activity)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(2, allowedPaymentMethods.length());
        assertEquals("PAYPAL", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(1)
                .getString("type"));
    }

    @Test
    public void requestPayment_whenPayPalDisabledByRequest_tokenizationPropertiesLackPayPal() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantName("google-merchant-name-override")
                .paypalEnabled(false);

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(activity)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(1, allowedPaymentMethods.length());
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
    }

    @Test
    public void requestPayment_whenPayPalDisabledInConfigurationAndGooglePayHasPayPalClientId_tokenizationPropertiesContainPayPal() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .paypalEnabled(false)
                .paypal(new TestConfigurationBuilder.TestPayPalConfigurationBuilder(false))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(activity)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(2, allowedPaymentMethods.length());
        assertEquals("PAYPAL", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(1)
                .getString("type"));
    }

    @Test
    public void requestPayment_usesGooglePaymentConfigurationClientId() throws JSONException, InvalidArgumentException {
         Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                 .paypal(new TestConfigurationBuilder.TestPayPalConfigurationBuilder(true)
                         .clientId("paypal-client-id-for-paypal"))
                 .withAnalytics()
                 .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(activity)
                .getJSONArray("allowedPaymentMethods");

        JSONObject paypal = allowedPaymentMethods.getJSONObject(0);

        assertEquals("paypal-client-id-for-google-payment",
                paypal.getJSONObject("parameters")
                        .getJSONObject("purchase_context")
                        .getJSONArray("purchase_units")
                        .getJSONObject(0)
                        .getJSONObject("payee")
                        .getString("client_id"));

        assertEquals("paypal-client-id-for-google-payment",
                paypal.getJSONObject("tokenizationSpecification")
                        .getJSONObject("parameters")
                        .getString("braintree:paypalClientId"));
    }

    @Test
    public void requestPayment_whenGooglePaymentConfigurationLacksClientId_tokenizationPropertiesLackPayPal() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(activity)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(1, allowedPaymentMethods.length());
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));

        assertFalse(allowedPaymentMethods.toString().contains("paypal-client-id-for-google-payment"));
    }

    @Test
    public void requestPayment_whenRequestIsNull_fowardsExceptionToCallback() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, null, requestPaymentCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(requestPaymentCallback).onResult(eq(false), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("Cannot pass null GooglePaymentRequest to requestPayment", exception.getMessage());
    }

    @Test
    public void requestPayment_whenManifestInvalid_fowardsExceptionToCallback() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(null)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest();
        FragmentActivity activity = mock(FragmentActivity.class);
        sut.requestPayment(activity, googlePaymentRequest, requestPaymentCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(requestPaymentCallback).onResult(eq(false), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("GooglePaymentActivity was not found in the Android " +
                "manifest, or did not have a theme of R.style.bt_transparent_activity", exception.getMessage());
    }

    // endregion

    // region tokenize

    @Test
    public void tokenize_withCardToken_returnsGooglePaymentNonce() throws InvalidArgumentException {
        String paymentDataJson = Fixtures.RESPONSE_GOOGLE_PAYMENT_CARD;

        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        PaymentData pd = PaymentData.fromJson(paymentDataJson);

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.tokenize(activity, pd, activityResultCallback);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(activityResultCallback).onResult(captor.capture(), (Exception)isNull());

        assertTrue(captor.getValue() instanceof GooglePaymentCardNonce);
    }

    @Test
    public void tokenize_withPayPalToken_returnsPayPalAccountNonce() throws InvalidArgumentException {
        String paymentDataJson = Fixtures.REPSONSE_GOOGLE_PAYMENT_PAYPAL_ACCOUNT;

        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        PaymentData pd = PaymentData.fromJson(paymentDataJson);

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.tokenize(activity, pd, activityResultCallback);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(activityResultCallback).onResult(captor.capture(), (Exception)isNull());

        assertTrue(captor.getValue() instanceof PayPalAccountNonce);
    }

    // endregion

    // region onActivityResult

    @Test
    public void onActivityResult_sendsAnalyticsEventOnCancel() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.onActivityResult(activity, RESULT_CANCELED, new Intent(), activityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.canceled"));
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnNonOkOrCanceledResult() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.onActivityResult(activity, RESULT_FIRST_USER, new Intent(), activityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnOkResponse() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.onActivityResult(activity, RESULT_OK, new Intent(), activityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.authorized"));
    }

    // endregion

    // region getAllowedCardNetworks
    @Test
    public void getAllowedCardNetworks_returnsSupportedNetworks() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        Collection<Integer> allowedCardNetworks = sut.getAllowedCardNetworks(configuration);

        assertEquals(4, allowedCardNetworks.size());
        assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_VISA));
        assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_MASTERCARD));
        assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_AMEX));
        assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_DISCOVER));
    }

    // endregion

    // region getTokenizationParameters

    @Test
    public void getTokenizationParameters_returnsCorrectParameters() throws Exception {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        Bundle tokenizationParameters = sut.getTokenizationParameters(activity, configuration).getParameters();

        assertEquals("braintree", tokenizationParameters.getString("gateway"));
        assertEquals(configuration.getMerchantId(), tokenizationParameters.getString("braintree:merchantId"));
        assertEquals(configuration.getGooglePayment().getGoogleAuthorizationFingerprint(),
                tokenizationParameters.getString("braintree:authorizationFingerprint"));
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME, tokenizationParameters.getString("braintree:sdkVersion"));
    }

    @Test
    public void getTokenizationParameters_doesNotIncludeATokenizationKeyWhenNotPresent() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        Bundle tokenizationParameters = sut.getTokenizationParameters(activity, configuration).getParameters();
        assertNull(tokenizationParameters.getString("braintree:clientKey"));
    }

    @Test
    public void getTokenizationParameters_includesATokenizationKeyWhenPresent() throws Exception {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        Bundle tokenizationParameters = sut.getTokenizationParameters(activity, configuration).getParameters();
        assertEquals(Fixtures.TOKENIZATION_KEY, tokenizationParameters.getString("braintree:clientKey"));
    }

    @Test
    public void getTokenizationParameters_forwardsParametersAndAllowedCardsToCallback() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorization(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        GooglePaymentGetTokenizationParametersCallback getTokenizationParametersCallback = mock(GooglePaymentGetTokenizationParametersCallback.class);
        sut.getTokenizationParameters(activity, getTokenizationParametersCallback);

        verify(getTokenizationParametersCallback).onResult(any(PaymentMethodTokenizationParameters.class), eq(sut.getAllowedCardNetworks(configuration)) );
    }

    // endregion

    private JSONObject getPaymentDataRequestJsonSentToGooglePayment(FragmentActivity activity) {
        JSONObject result = new JSONObject();
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        try {
            result = new JSONObject(paymentDataRequest.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }
}