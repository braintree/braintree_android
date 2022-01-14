package com.braintreepayments.api;

import android.content.Intent;
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

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.GooglePayClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePayClient.EXTRA_PAYMENT_DATA_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GooglePayClientUnitTest {

    private FragmentActivity activity;

    private GooglePayRequest baseRequest;

    private GooglePayIsReadyToPayCallback readyToPayCallback;
    private GooglePayRequestPaymentCallback requestPaymentCallback;
    private GooglePayOnActivityResultCallback activityResultCallback;

    private ActivityInfo activityInfo;

    @Before
    public void beforeEach() {
        activity = mock(FragmentActivity.class);
        readyToPayCallback = mock(GooglePayIsReadyToPayCallback.class);
        requestPaymentCallback = mock(GooglePayRequestPaymentCallback.class);
        activityResultCallback = mock(GooglePayOnActivityResultCallback.class);
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.isReadyToPay(activity, null, readyToPayCallback);

        ArgumentCaptor<IsReadyToPayRequest> captor = ArgumentCaptor.forClass(IsReadyToPayRequest.class);
        verify(internalGooglePayClient).isReadyToPay(same(activity), same(configuration), captor.capture(), any(GooglePayIsReadyToPayCallback.class));

        String actualJson = captor.getValue().toJson();
        JSONAssert.assertEquals(
                Fixtures.READY_TO_PAY_REQUEST_WITHOUT_EXISTING_PAYMENT_METHOD, actualJson, false);
    }

    @Test
    public void isReadyToPay_whenExistingPaymentMethodRequired_sendsIsReadyToPayRequestWithExistingPaymentRequired() throws JSONException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.isReadyToPay(activity, readyForGooglePayRequest, readyToPayCallback);

        ArgumentCaptor<IsReadyToPayRequest> captor = ArgumentCaptor.forClass(IsReadyToPayRequest.class);
        verify(internalGooglePayClient).isReadyToPay(same(activity), same(configuration), captor.capture(), any(GooglePayIsReadyToPayCallback.class));

        String actualJson = captor.getValue().toJson();
        JSONAssert.assertEquals(
                Fixtures.READY_TO_PAY_REQUEST_WITH_EXISTING_PAYMENT_METHOD, actualJson, false);
    }

    @Test
    public void isReadyToPay_returnsFalseWhenGooglePayIsNotEnabled() throws Exception {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder().enabled(false))
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
        verify(readyToPayCallback).onResult(false, null);
    }

    @Test
    public void isReadyToPay_whenActivityIsNull_forwardsErrorToCallback() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder().enabled(true))
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.isReadyToPay(null, null, readyToPayCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(readyToPayCallback).onResult(eq(false), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof IllegalArgumentException);
        assertEquals("Activity cannot be null.", exception.getMessage());
    }

    // endregion

    // region requestPayment

    @Test
    public void requestPayment_startsActivity() throws InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, baseRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAY));
    }

    @Test
    public void requestPayment_startsActivityWithOptionalValues() throws JSONException, InvalidArgumentException {
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
        googlePayRequest.setShippingAddressRequirements(ShippingAddressRequirements.newBuilder().addAllowedCountryCode("USA").build());
        googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
                .setTotalPrice("1.00")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setCurrencyCode("USD")
                .build());

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAY));
        Intent intent = captor.getValue();

        assertEquals(GooglePayActivity.class.getName(), intent.getComponent().getClassName());
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

        String googlePayModuleVersion = com.braintreepayments.api.googlepay.BuildConfig.VERSION_NAME;
        assertEquals(googlePayModuleVersion, paypalTokenizationSpecificationParams.getString("braintree:sdkVersion"));
        assertEquals("integration_merchant_id", paypalTokenizationSpecificationParams.getString("braintree:merchantId"));
        assertEquals("{\"source\":\"client\",\"version\":\"" + googlePayModuleVersion + "\",\"platform\":\"android\"}", paypalTokenizationSpecificationParams.getString("braintree:metadata"));
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
        assertEquals(googlePayModuleVersion, cardTokenizationSpecificationParams.getString("braintree:sdkVersion"));
        assertEquals("integration_merchant_id", cardTokenizationSpecificationParams.getString("braintree:merchantId"));
        assertEquals("{\"source\":\"client\",\"version\":\"" + googlePayModuleVersion + "\",\"platform\":\"android\"}", cardTokenizationSpecificationParams.getString("braintree:metadata"));
        assertEquals("sandbox_tokenization_string", cardTokenizationSpecificationParams.getString("braintree:clientKey"));
    }

    @Test
    public void requestPayment_includesATokenizationKeyWhenPresent() throws JSONException, InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAY));
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAY));
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        InOrder order = inOrder(braintreeClient);
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.selected"));
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.started"));
    }

    @Test
    public void requestPayment_postsExceptionWhenTransactionInfoIsNull() throws InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();
        GooglePayRequest googlePayRequest = new GooglePayRequest();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, baseRequest, requestPaymentCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(requestPaymentCallback).onResult(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("Google Pay is not enabled for your Braintree account, or Google Play Services are not configured correctly.",
                captor.getValue().getMessage());
    }

    @Test
    public void requestPayment_whenSandbox_setsTestEnvironment() throws JSONException, InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, baseRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAY));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        assertEquals("TEST", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void requestPayment_whenProduction_setsProductionEnvironment() throws JSONException, InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, baseRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAY));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_PRODUCTION, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        assertEquals("PRODUCTION", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void requestPayment_withGoogleMerchantId_sendGoogleMerchantId() throws JSONException, InvalidArgumentException {
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
        baseRequest.setGoogleMerchantId("google-merchant-id-override");

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAY));

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
        baseRequest.setGoogleMerchantName("google-merchant-name-override");

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAY));

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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePay(activity)
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePay(activity)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(1, allowedPaymentMethods.length());
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
    }

    @Test
    public void requestPayment_whenPayPalDisabledInConfigurationAndGooglePayHasPayPalClientId_tokenizationPropertiesContainPayPal() throws JSONException, InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePay(activity)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(2, allowedPaymentMethods.length());
        assertEquals("PAYPAL", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(1)
                .getString("type"));
    }

    @Test
    public void requestPayment_usesGooglePayConfigurationClientId() throws JSONException, InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePay(activity)
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
    public void requestPayment_whenGooglePayConfigurationLacksClientId_tokenizationPropertiesLackPayPal() throws JSONException, InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePay(activity)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(1, allowedPaymentMethods.length());
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));

        assertFalse(allowedPaymentMethods.toString().contains("paypal-client-id-for-google-payment"));
    }

    @Test
    public void requestPayment_whenConfigurationContainsElo_addsEloAndEloDebitToAllowedPaymentMethods() throws JSONException, InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        JSONArray allowedCardNetworks = getPaymentDataRequestJsonSentToGooglePay(activity)
                .getJSONArray("allowedPaymentMethods")
                .getJSONObject(0)
                .getJSONObject("parameters")
                .getJSONArray("allowedCardNetworks");

        assertEquals(2, allowedCardNetworks.length());
        assertEquals("ELO", allowedCardNetworks.getString(0));
        assertEquals("ELO_DEBIT", allowedCardNetworks.getString(1));
    }

    @Test
    public void requestPayment_whenRequestIsNull_fowardsExceptionToCallback() throws InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, null, requestPaymentCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(requestPaymentCallback).onResult(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("Cannot pass null GooglePayRequest to requestPayment", exception.getMessage());
    }

    @Test
    public void requestPayment_whenManifestInvalid_fowardsExceptionToCallback() throws InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        GooglePayRequest googlePayRequest = new GooglePayRequest();
        FragmentActivity activity = mock(FragmentActivity.class);
        sut.requestPayment(activity, googlePayRequest, requestPaymentCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(requestPaymentCallback).onResult(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("GooglePayActivity was not found in the Android " +
                "manifest, or did not have a theme of R.style.bt_transparent_activity", exception.getMessage());
    }

    // endregion

    // region tokenize

    @Test
    public void tokenize_withCardToken_returnsGooglePayNonce() throws InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.tokenize(pd, activityResultCallback);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(activityResultCallback).onResult(captor.capture(), (Exception) isNull());

        assertTrue(captor.getValue() instanceof GooglePayCardNonce);
    }

    @Test
    public void tokenize_withPayPalToken_returnsPayPalAccountNonce() throws InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.tokenize(pd, activityResultCallback);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(activityResultCallback).onResult(captor.capture(), (Exception) isNull());

        assertTrue(captor.getValue() instanceof PayPalAccountNonce);
    }

    // endregion

    // region onActivityResult

    @Test
    public void onActivityResult_OnCancel_sendsAnalyticsAndReturnsErrorToCallback() throws InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.onActivityResult(RESULT_CANCELED, new Intent(), activityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.canceled"));

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(activityResultCallback).onResult((PaymentMethodNonce) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertEquals("User canceled Google Pay.", exception.getMessage());
        assertTrue(exception instanceof UserCanceledException);
    }

    @Test
    public void onActivityResult_OnNonOkOrCanceledResult_sendsAnalyticsAndReturnsErrorToCallback() throws InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.onActivityResult(RESULT_FIRST_USER, new Intent(), activityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));

        ArgumentCaptor<GooglePayException> captor = ArgumentCaptor.forClass(GooglePayException.class);
        verify(activityResultCallback).onResult((PaymentMethodNonce) isNull(), captor.capture());

        GooglePayException exception = captor.getValue();
        assertEquals("An error was encountered during the Google Pay flow. See the status object in this exception for more details.", exception.getMessage());
        assertTrue(exception instanceof BraintreeException);
    }

    @Test
    public void onActivityResult_OnOkResponse_sendsAnalytics() throws InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);
        sut.onActivityResult(RESULT_OK, new Intent(), activityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.authorized"));
    }

    // endregion

    // region getAllowedCardNetworks
    @Test
    public void getAllowedCardNetworks_returnsSupportedNetworks() throws InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

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
    public void getTokenizationParameters_returnsCorrectParameters() throws Exception {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();
        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        Bundle tokenizationParameters = sut.getTokenizationParameters(configuration, ).getParameters();

        assertEquals("braintree", tokenizationParameters.getString("gateway"));
        assertEquals(configuration.getMerchantId(), tokenizationParameters.getString("braintree:merchantId"));
        assertEquals(configuration.getGooglePayAuthorizationFingerprint(),
                tokenizationParameters.getString("braintree:authorizationFingerprint"));
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME, tokenizationParameters.getString("braintree:sdkVersion"));
    }

    @Test
    public void getTokenizationParameters_doesNotIncludeATokenizationKeyWhenNotPresent() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .withAnalytics()
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();
        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        Bundle tokenizationParameters = sut.getTokenizationParameters(configuration, ).getParameters();
        assertNull(tokenizationParameters.getString("braintree:clientKey"));
    }

    @Test
    public void getTokenizationParameters_includesATokenizationKeyWhenPresent() throws Exception {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();
        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        Bundle tokenizationParameters = sut.getTokenizationParameters(configuration, ).getParameters();
        assertEquals(Fixtures.TOKENIZATION_KEY, tokenizationParameters.getString("braintree:clientKey"));
    }

    @Test
    public void getTokenizationParameters_forwardsParametersAndAllowedCardsToCallback() throws InvalidArgumentException {
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();
        GooglePayClient sut = new GooglePayClient(braintreeClient, internalGooglePayClient);

        GooglePayGetTokenizationParametersCallback getTokenizationParametersCallback = mock(GooglePayGetTokenizationParametersCallback.class);
        sut.getTokenizationParameters(getTokenizationParametersCallback);

        verify(getTokenizationParametersCallback).onResult(any(PaymentMethodTokenizationParameters.class), eq(sut.getAllowedCardNetworks(configuration)));
    }

    // endregion

    private JSONObject getPaymentDataRequestJsonSentToGooglePay(FragmentActivity activity) {
        JSONObject result = new JSONObject();
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAY));

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