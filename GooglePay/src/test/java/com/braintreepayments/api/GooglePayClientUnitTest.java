package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.googlepay.R;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collection;

@RunWith(RobolectricTestRunner.class)
public class GooglePayClientUnitTest {

    private FragmentActivity activity;

    private GooglePayRequest baseRequest;

    private GooglePayIsReadyToPayCallback readyToPayCallback;
    private GooglePayPaymentAuthRequestCallback intentDataCallback;
    private GooglePayTokenizeCallback activityResultCallback;
    private ActivityInfo activityInfo;

    @Before
    public void beforeEach() {
        activity = mock(FragmentActivity.class);
        readyToPayCallback = mock(GooglePayIsReadyToPayCallback.class);
        activityResultCallback = mock(GooglePayTokenizeCallback.class);
        intentDataCallback = mock(GooglePayPaymentAuthRequestCallback.class);
        activityInfo = mock(ActivityInfo.class);

        baseRequest = new GooglePayRequest();
        baseRequest.setTransactionInfo(TransactionInfo.newBuilder()
                .setTotalPrice("1.00")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setCurrencyCode("USD")
                .build());

        when(activityInfo.getThemeResource()).thenReturn(R.style.bt_transparent_activity);
    }

    // region isReadyToPay

    @Test
    public void isReadyToPay_sendsReadyToPayRequest() throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .supportedNetworks(new String[]{"amex", "visa"})
                        .enabled(true))
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.isReadyToPay(activity, null, readyToPayCallback);

        ArgumentCaptor<IsReadyToPayRequest> captor =
                ArgumentCaptor.forClass(IsReadyToPayRequest.class);
        verify(internalGooglePayClient).isReadyToPay(same(activity), same(configuration),
                captor.capture(), any(GooglePayIsReadyToPayCallback.class));

        String actualJson = captor.getValue().toJson();
        JSONAssert.assertEquals(
                Fixtures.READY_TO_PAY_REQUEST_WITHOUT_EXISTING_PAYMENT_METHOD, actualJson, false);
    }

    @Test
    public void isReadyToPay_whenExistingPaymentMethodRequired_sendsIsReadyToPayRequestWithExistingPaymentRequired()
            throws JSONException {
        ReadyForGooglePayRequest readyForGooglePayRequest = new ReadyForGooglePayRequest();
        readyForGooglePayRequest.setExistingPaymentMethodRequired(true);

        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .supportedNetworks(new String[]{"amex", "visa"})
                        .enabled(true))
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.isReadyToPay(activity, readyForGooglePayRequest, readyToPayCallback);

        ArgumentCaptor<IsReadyToPayRequest> captor =
                ArgumentCaptor.forClass(IsReadyToPayRequest.class);
        verify(internalGooglePayClient).isReadyToPay(same(activity), same(configuration),
                captor.capture(), any(GooglePayIsReadyToPayCallback.class));

        String actualJson = captor.getValue().toJson();
        JSONAssert.assertEquals(
                Fixtures.READY_TO_PAY_REQUEST_WITH_EXISTING_PAYMENT_METHOD, actualJson, false);
    }

    @Test
    public void isReadyToPay_returnsFalseWhenGooglePayIsNotEnabled() {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder().enabled(
                        false))
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder()
                .isReadyToPay(true)
                .build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        sut.isReadyToPay(activity, null, readyToPayCallback);
        verify(readyToPayCallback).onGooglePayReadinessResult(any(GooglePayReadinessResult.NotReadyToPay.class));
    }

    @Test
    public void isReadyToPay_whenActivityIsNull_forwardsErrorToCallback() {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder().enabled(
                        true))
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.isReadyToPay(null, null, readyToPayCallback);

        ArgumentCaptor<GooglePayReadinessResult> captor = ArgumentCaptor.forClass(GooglePayReadinessResult.class);
        verify(readyToPayCallback).onGooglePayReadinessResult(captor.capture());

        GooglePayReadinessResult result = captor.getValue();
        assertTrue(result instanceof GooglePayReadinessResult.NotReadyToPay);
        Throwable exception = ((GooglePayReadinessResult.NotReadyToPay) result).getError();
        assertTrue(exception instanceof IllegalArgumentException);
        assertEquals("Activity cannot be null.", exception.getMessage());
    }

    // endregion

    // region createPaymentAuthRequest

    @Test
    public void createPaymentAuthRequest_callsBackIntentData() throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayRequest googlePayRequest = new GooglePayRequest();
        googlePayRequest.setAllowPrepaidCards(true);
        googlePayRequest.setBillingAddressFormat(1);
        googlePayRequest.setBillingAddressRequired(true);
        googlePayRequest.setEmailRequired(true);
        googlePayRequest.setPhoneNumberRequired(true);
        googlePayRequest.setShippingAddressRequired(true);
        googlePayRequest.setShippingAddressRequirements(
                ShippingAddressRequirements.newBuilder().addAllowedCountryCode("USA").build());
        googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
                .setTotalPrice("1.00")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setCurrencyCode("USD")
                .build());

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(googlePayRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.ReadyToLaunch);
        GooglePayPaymentAuthRequestParams intent = ((GooglePayPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getGooglePayEnvironment());
        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();

        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(2, paymentDataRequestJson.get("apiVersion"));
        assertEquals(0, paymentDataRequestJson.get("apiVersionMinor"));

        assertEquals(true, paymentDataRequestJson.get("emailRequired"));
        assertEquals(true, paymentDataRequestJson.get("shippingAddressRequired"));

        JSONObject transactionInfoJson = paymentDataRequestJson.getJSONObject("transactionInfo");
        assertEquals("FINAL", transactionInfoJson.getString("totalPriceStatus"));
        assertEquals("1.00", transactionInfoJson.getString("totalPrice"));
        assertEquals("USD", transactionInfoJson.getString("currencyCode"));

        JSONArray allowedPaymentMethods =
                paymentDataRequestJson.getJSONArray("allowedPaymentMethods");
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

        JSONObject paypalTokenizationSpecification =
                paypal.getJSONObject("tokenizationSpecification");
        assertEquals("PAYMENT_GATEWAY", paypalTokenizationSpecification.getString("type"));

        JSONObject paypalTokenizationSpecificationParams =
                paypalTokenizationSpecification.getJSONObject("parameters");
        assertEquals("braintree", paypalTokenizationSpecificationParams.getString("gateway"));
        assertEquals("v1", paypalTokenizationSpecificationParams.getString("braintree:apiVersion"));

        String googlePayModuleVersion =
                com.braintreepayments.api.googlepay.BuildConfig.VERSION_NAME;
        assertEquals(googlePayModuleVersion,
                paypalTokenizationSpecificationParams.getString("braintree:sdkVersion"));
        assertEquals("integration_merchant_id",
                paypalTokenizationSpecificationParams.getString("braintree:merchantId"));
        assertEquals("{\"source\":\"client\",\"version\":\"" + googlePayModuleVersion +
                        "\",\"platform\":\"android\"}",
                paypalTokenizationSpecificationParams.getString("braintree:metadata"));
        assertFalse(paypalTokenizationSpecificationParams.has("braintree:clientKey"));
        assertEquals("paypal-client-id-for-google-payment",
                paypalTokenizationSpecificationParams.getString("braintree:paypalClientId"));

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

        JSONObject cardTokenizationSpecificationParams =
                tokenizationSpecification.getJSONObject("parameters");
        assertEquals("braintree", cardTokenizationSpecificationParams.getString("gateway"));
        assertEquals("v1", cardTokenizationSpecificationParams.getString("braintree:apiVersion"));
        assertEquals(googlePayModuleVersion,
                cardTokenizationSpecificationParams.getString("braintree:sdkVersion"));
        assertEquals("integration_merchant_id",
                cardTokenizationSpecificationParams.getString("braintree:merchantId"));
        assertEquals("{\"source\":\"client\",\"version\":\"" + googlePayModuleVersion +
                        "\",\"platform\":\"android\"}",
                cardTokenizationSpecificationParams.getString("braintree:metadata"));
        assertEquals("sandbox_tokenization_string",
                cardTokenizationSpecificationParams.getString("braintree:clientKey"));
    }

    @Test
    public void createPaymentAuthRequest_includesATokenizationKeyWhenPresent() throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .activityInfo(activityInfo)
                .build();

        GooglePayRequest googlePayRequest = new GooglePayRequest();
        googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
                .setTotalPrice("1.00")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setCurrencyCode("USD")
                .build());

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(googlePayRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.ReadyToLaunch);
        GooglePayPaymentAuthRequestParams intent = ((GooglePayPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedPaymentMethods =
                paymentDataRequestJson.getJSONArray("allowedPaymentMethods");
        JSONObject card = allowedPaymentMethods.getJSONObject(0);
        JSONObject tokenizationSpecification = card.getJSONObject("tokenizationSpecification");
        JSONObject cardTokenizationSpecificationParams =
                tokenizationSpecification.getJSONObject("parameters");
        assertEquals(Fixtures.TOKENIZATION_KEY,
                cardTokenizationSpecificationParams.get("braintree:clientKey"));
    }

    @Test
    public void createPaymentAuthRequest_doesNotIncludeATokenizationKeyWhenNotPresent() throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .activityInfo(activityInfo)
                .build();

        GooglePayRequest googlePayRequest = new GooglePayRequest();
        googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
                .setTotalPrice("1.00")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setCurrencyCode("USD")
                .build());

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(googlePayRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.ReadyToLaunch);
        GooglePayPaymentAuthRequestParams intent = ((GooglePayPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedPaymentMethods =
                paymentDataRequestJson.getJSONArray("allowedPaymentMethods");
        JSONObject card = allowedPaymentMethods.getJSONObject(0);
        JSONObject tokenizationSpecification = card.getJSONObject("tokenizationSpecification");
        JSONObject cardTokenizationSpecificationParams =
                tokenizationSpecification.getJSONObject("parameters");
        assertFalse(cardTokenizationSpecificationParams.has("braintree:clientKey"));
    }

    @Test
    public void createPaymentAuthRequest_sendsAnalyticsEvent() {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayRequest googlePayRequest = new GooglePayRequest();
        googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
                .setTotalPrice("1.00")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setCurrencyCode("USD")
                .build());

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(googlePayRequest, intentDataCallback);

        InOrder order = inOrder(braintreeClient);
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.selected"));
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.started"));
    }

    @Test
    public void createPaymentAuthRequest_postsExceptionWhenTransactionInfoIsNull() {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();
        GooglePayRequest googlePayRequest = new GooglePayRequest();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(googlePayRequest, intentDataCallback);

        InOrder order = inOrder(braintreeClient);
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.selected"));
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));
    }

    @Test
    public void createPaymentAuthRequest_whenMerchantNotConfigured_returnsExceptionToFragment() {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(configuration)
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.Failure);
        Exception exception = ((GooglePayPaymentAuthRequest.Failure) request).getError();

        assertTrue(exception instanceof BraintreeException);
        assertEquals(
                "Google Pay is not enabled for your Braintree account, or Google Play Services are not configured correctly.",
                exception.getMessage());
    }

    @Test
    public void createPaymentAuthRequest_whenSandbox_setsTestEnvironment() throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.ReadyToLaunch);
        GooglePayPaymentAuthRequestParams intent = ((GooglePayPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getGooglePayEnvironment());
        assertEquals("TEST", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void createPaymentAuthRequest_whenProduction_setsProductionEnvironment() throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("production")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.ReadyToLaunch);
        GooglePayPaymentAuthRequestParams intent = ((GooglePayPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_PRODUCTION, intent.getGooglePayEnvironment());
        assertEquals("PRODUCTION", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void createPaymentAuthRequest_withGoogleMerchantName_sendGoogleMerchantName()
            throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        baseRequest.setGoogleMerchantName("google-merchant-name-override");

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.ReadyToLaunch);
        GooglePayPaymentAuthRequestParams intent = ((GooglePayPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals("google-merchant-name-override", paymentDataRequestJson
                .getJSONObject("merchantInfo")
                .getString("merchantName"));
    }

    @Test
    public void createPaymentAuthRequest_whenGooglePayCanProcessPayPal_tokenizationPropertiesIncludePayPal()
            throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayRequest googlePayRequest = baseRequest;
        googlePayRequest.setGoogleMerchantName("google-merchant-name-override");

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.ReadyToLaunch);
        GooglePayPaymentAuthRequestParams intent = ((GooglePayPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());
        JSONArray allowedPaymentMethods = paymentDataRequestJson
                .getJSONArray("allowedPaymentMethods");

        assertEquals(2, allowedPaymentMethods.length());
        assertEquals("PAYPAL", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(1)
                .getString("type"));
    }

    @Test
    public void createPaymentAuthRequest_whenPayPalDisabledByRequest_tokenizationPropertiesLackPayPal()
            throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayRequest googlePayRequest = baseRequest;
        googlePayRequest.setGoogleMerchantName("google-merchant-name-override");
        googlePayRequest.setPayPalEnabled(false);

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.ReadyToLaunch);
        GooglePayPaymentAuthRequestParams intent = ((GooglePayPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedPaymentMethods = paymentDataRequestJson
                .getJSONArray("allowedPaymentMethods");

        assertEquals(1, allowedPaymentMethods.length());
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
    }

    @Test
    public void createPaymentAuthRequest_whenPayPalDisabledInConfigurationAndGooglePayHasPayPalClientId_tokenizationPropertiesContainPayPal()
            throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
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
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayRequest googlePayRequest = baseRequest;
        googlePayRequest.setGoogleMerchantName("google-merchant-name-override");

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.ReadyToLaunch);
        GooglePayPaymentAuthRequestParams intent = ((GooglePayPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedPaymentMethods = paymentDataRequestJson
                .getJSONArray("allowedPaymentMethods");

        assertEquals(2, allowedPaymentMethods.length());
        assertEquals("PAYPAL", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(1)
                .getString("type"));
    }

    @Test
    public void createPaymentAuthRequest_usesGooglePayConfigurationClientId() throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
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
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayRequest googlePayRequest = baseRequest;
        googlePayRequest.setGoogleMerchantName("google-merchant-name-override");

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.ReadyToLaunch);
        GooglePayPaymentAuthRequestParams intent = ((GooglePayPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedPaymentMethods = paymentDataRequestJson
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
    public void createPaymentAuthRequest_whenGooglePayConfigurationLacksClientId_tokenizationPropertiesLackPayPal()
            throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayRequest googlePayRequest = baseRequest;
        googlePayRequest.setGoogleMerchantName("google-merchant-name-override");

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.ReadyToLaunch);
        GooglePayPaymentAuthRequestParams intent = ((GooglePayPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedPaymentMethods = paymentDataRequestJson
                .getJSONArray("allowedPaymentMethods");

        assertEquals(1, allowedPaymentMethods.length());
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));

        assertFalse(
                allowedPaymentMethods.toString().contains("paypal-client-id-for-google-payment"));
    }

    @Test
    public void createPaymentAuthRequest_whenConfigurationContainsElo_addsEloAndEloDebitToAllowedPaymentMethods()
            throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"elo"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayRequest googlePayRequest = baseRequest;
        googlePayRequest.setGoogleMerchantName("google-merchant-name-override");

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.ReadyToLaunch);
        GooglePayPaymentAuthRequestParams intent = ((GooglePayPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedCardNetworks = paymentDataRequestJson
                .getJSONArray("allowedPaymentMethods")
                .getJSONObject(0)
                .getJSONObject("parameters")
                .getJSONArray("allowedCardNetworks");

        assertEquals(2, allowedCardNetworks.length());
        assertEquals("ELO", allowedCardNetworks.getString(0));
        assertEquals("ELO_DEBIT", allowedCardNetworks.getString(1));
    }

    @Test
    public void createPaymentAuthRequest_whenRequestIsNull_forwardsExceptionToListener() {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(null, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.Failure);
        Exception exception = ((GooglePayPaymentAuthRequest.Failure) request).getError();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("Cannot pass null GooglePayRequest to requestPayment", exception.getMessage());
    }

    @Test
    public void createPaymentAuthRequest_whenManifestInvalid_forwardsExceptionToListener() {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(null)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback);

        ArgumentCaptor<GooglePayPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                GooglePayPaymentAuthRequest.class);
        verify(intentDataCallback).onGooglePayPaymentAuthRequest(captor.capture());

        GooglePayPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof GooglePayPaymentAuthRequest.Failure);
        Exception exception = ((GooglePayPaymentAuthRequest.Failure) request).getError();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("GooglePayActivity was not found in the Android " +
                        "manifest, or did not have a theme of R.style.bt_transparent_activity",
                exception.getMessage());
    }

    // endregion

    // region internal tokenize

    @Test
    public void tokenize_withCardToken_returnsGooglePayNonce() {
        String paymentDataJson = Fixtures.RESPONSE_GOOGLE_PAY_CARD;

        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        PaymentData pd = PaymentData.fromJson(paymentDataJson);

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.tokenize(pd, activityResultCallback);

        ArgumentCaptor<GooglePayResult> captor =
                ArgumentCaptor.forClass(GooglePayResult.class);
        verify(activityResultCallback).onGooglePayResult(captor.capture());

        GooglePayResult result = captor.getValue();

        assertTrue(result instanceof GooglePayResult.Success);
        assertTrue(((GooglePayResult.Success) result).getNonce() instanceof GooglePayCardNonce);
    }

    @Test
    public void tokenize_withPayPalToken_returnsPayPalAccountNonce() {
        String paymentDataJson = Fixtures.REPSONSE_GOOGLE_PAY_PAYPAL_ACCOUNT;

        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        PaymentData pd = PaymentData.fromJson(paymentDataJson);

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.tokenize(pd, activityResultCallback);

        ArgumentCaptor<GooglePayResult> captor =
                ArgumentCaptor.forClass(GooglePayResult.class);
        verify(activityResultCallback).onGooglePayResult(captor.capture());

        GooglePayResult result = captor.getValue();
        assertTrue(result instanceof GooglePayResult.Success);
        assertTrue(((GooglePayResult.Success) result).getNonce() instanceof PayPalAccountNonce);
    }

    // endregion

    // region public tokenize

    @Test
    public void tokenize_whenPaymentDataExists_returnsResultToListener_andSendsAnalytics()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        String paymentDataJson = Fixtures.RESPONSE_GOOGLE_PAY_CARD;
        PaymentData paymentData = PaymentData.fromJson(paymentDataJson);
        GooglePayPaymentAuthResult
                googlePayPaymentAuthResult = new GooglePayPaymentAuthResult(paymentData, null);
        sut.tokenize(googlePayPaymentAuthResult, activityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.authorized"));
        ArgumentCaptor<GooglePayResult> captor =
                ArgumentCaptor.forClass(GooglePayResult.class);
        verify(activityResultCallback).onGooglePayResult(captor.capture());

        GooglePayResult result = captor.getValue();
        assertTrue(result instanceof GooglePayResult.Success);
        PaymentMethodNonce nonce = ((GooglePayResult.Success) result).getNonce();
        PaymentMethodNonce expectedNonce = GooglePayCardNonce.fromJSON(new JSONObject(paymentDataJson));
        assertEquals(nonce.getString(), expectedNonce.getString());
    }

    @Test
    public void tokenize_whenErrorExists_returnsErrorToListener_andSendsAnalytics() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        Exception error = new Exception("Error");
        GooglePayPaymentAuthResult
                googlePayPaymentAuthResult = new GooglePayPaymentAuthResult(null, error);
        sut.tokenize(googlePayPaymentAuthResult, activityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));

        ArgumentCaptor<GooglePayResult> captor = ArgumentCaptor.forClass(GooglePayResult.class);
        verify(activityResultCallback).onGooglePayResult(captor.capture());

        GooglePayResult result = captor.getValue();
        assertTrue(result instanceof GooglePayResult.Failure);
        assertEquals(error, ((GooglePayResult.Failure) result).getError());
    }

    @Test
    public void tokenize_whenUserCanceledErrorExists_returnsErrorToListener_andSendsAnalytics() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        UserCanceledException userCanceledError =
                new UserCanceledException("User canceled Google Pay.");
        GooglePayPaymentAuthResult
                googlePayPaymentAuthResult = new GooglePayPaymentAuthResult(null, userCanceledError);
        sut.tokenize(googlePayPaymentAuthResult, activityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.canceled"));

        ArgumentCaptor<GooglePayResult> captor = ArgumentCaptor.forClass(GooglePayResult.class);
        verify(activityResultCallback).onGooglePayResult(captor.capture());

        GooglePayResult result = captor.getValue();
        assertTrue(result instanceof GooglePayResult.Cancel);
    }

    // endregion

    // region getAllowedCardNetworks
    @Test
    public void getAllowedCardNetworks_returnsSupportedNetworks() {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

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
    public void getTokenizationParameters_returnsCorrectParameters() {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .withAnalytics()
                .buildConfiguration();

        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(authorization)
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();
        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        Bundle tokenizationParameters =
                sut.getTokenizationParameters(configuration, authorization).getParameters();

        assertEquals("braintree", tokenizationParameters.getString("gateway"));
        assertEquals(configuration.getMerchantId(),
                tokenizationParameters.getString("braintree:merchantId"));
        assertEquals(configuration.getGooglePayAuthorizationFingerprint(),
                tokenizationParameters.getString("braintree:authorizationFingerprint"));
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME,
                tokenizationParameters.getString("braintree:sdkVersion"));
    }

    @Test
    public void getTokenizationParameters_doesNotIncludeATokenizationKeyWhenNotPresent() {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .withAnalytics()
                .buildConfiguration();

        Authorization authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(authorization)
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();
        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        Bundle tokenizationParameters =
                sut.getTokenizationParameters(configuration, authorization).getParameters();
        assertNull(tokenizationParameters.getString("braintree:clientKey"));
    }

    @Test
    public void getTokenizationParameters_includesATokenizationKeyWhenPresent() {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .withAnalytics()
                .buildConfiguration();

        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(authorization)
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();
        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        Bundle tokenizationParameters =
                sut.getTokenizationParameters(configuration, authorization).getParameters();
        assertEquals(Fixtures.TOKENIZATION_KEY,
                tokenizationParameters.getString("braintree:clientKey"));
    }

    @Test
    public void getTokenizationParameters_forwardsParametersAndAllowedCardsToCallback() {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient =
                new MockGooglePayInternalClientBuilder().build();
        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        sut.getTokenizationParameters(tokenizationParameters -> {
            assertTrue(tokenizationParameters instanceof GooglePayTokenizationParameters.Success);
            assertNotNull(((GooglePayTokenizationParameters.Success) tokenizationParameters).getParameters());
            assertEquals(sut.getAllowedCardNetworks(configuration), ((GooglePayTokenizationParameters.Success) tokenizationParameters).getAllowedCardNetworks());
        });
    }

    // endregion
}