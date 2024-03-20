package com.braintreepayments.api;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.GooglePayClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePayClient.EXTRA_PAYMENT_DATA_REQUEST;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.activity.result.ActivityResultRegistry;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

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

    private Context applicationContext;
    private Lifecycle lifecycle;

    private GooglePayRequest baseRequest;

    private GooglePayIsReadyToPayCallback readyToPayCallback;
    private GooglePayRequestPaymentCallback requestPaymentCallback;
    private GooglePayOnActivityResultCallback activityResultCallback;
    private GooglePayListener listener;

    private ActivityInfo activityInfo;

    @Before
    public void beforeEach() {
        activity = mock(FragmentActivity.class);
        applicationContext = mock(Context.class);
        when(activity.getApplicationContext()).thenReturn(applicationContext);

        lifecycle = mock(Lifecycle.class);
        readyToPayCallback = mock(GooglePayIsReadyToPayCallback.class);
        requestPaymentCallback = mock(GooglePayRequestPaymentCallback.class);
        activityResultCallback = mock(GooglePayOnActivityResultCallback.class);
        listener = mock(GooglePayListener.class);
        activityInfo = mock(ActivityInfo.class);

        baseRequest = new GooglePayRequest();
        baseRequest.setTransactionInfo(TransactionInfo.newBuilder()
                .setTotalPrice("1.00")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setCurrencyCode("USD")
                .build());

        when(activityInfo.getThemeResource()).thenReturn(R.style.bt_transparent_activity);
    }

    // region Constructor

    @Test
    public void constructor_withFragment_passesFragmentLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ActivityResultRegistry resultRegistry = mock(ActivityResultRegistry.class);
        when(activity.getActivityResultRegistry()).thenReturn(resultRegistry);
        Fragment fragment = mock(Fragment.class);
        when(fragment.requireActivity()).thenReturn(activity);
        when(fragment.getLifecycle()).thenReturn(lifecycle);

        GooglePayClient sut = new GooglePayClient(fragment, braintreeClient);
        ArgumentCaptor<GooglePayLifecycleObserver> captor = ArgumentCaptor.forClass(GooglePayLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        GooglePayLifecycleObserver observer = captor.getValue();
        assertSame(resultRegistry, observer.activityResultRegistry);
        assertSame(sut, observer.googlePayClient);
    }

    @Test
    public void constructor_withFragmentActivity_passesActivityLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ActivityResultRegistry resultRegistry = mock(ActivityResultRegistry.class);
        when(activity.getLifecycle()).thenReturn(lifecycle);
        when(activity.getActivityResultRegistry()).thenReturn(resultRegistry);

        GooglePayClient sut = new GooglePayClient(activity, braintreeClient);
        ArgumentCaptor<GooglePayLifecycleObserver> captor = ArgumentCaptor.forClass(GooglePayLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        GooglePayLifecycleObserver observer = captor.getValue();
        assertSame(resultRegistry, observer.activityResultRegistry);
        assertSame(sut, observer.googlePayClient);
    }

    @Test
    public void constructor_withoutFragmentOrActivity_doesNotSetObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(braintreeClient);

        verify(lifecycle, never()).addObserver(any(GooglePayLifecycleObserver.class));
    }

    // endregion

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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.isReadyToPay(activity, null, readyToPayCallback);

        ArgumentCaptor<IsReadyToPayRequest> captor = ArgumentCaptor.forClass(IsReadyToPayRequest.class);
        verify(internalGooglePayClient).isReadyToPay(same(applicationContext), same(configuration), captor.capture(), any(GooglePayIsReadyToPayCallback.class));

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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.isReadyToPay(activity, readyForGooglePayRequest, readyToPayCallback);

        ArgumentCaptor<IsReadyToPayRequest> captor = ArgumentCaptor.forClass(IsReadyToPayRequest.class);
        verify(internalGooglePayClient).isReadyToPay(same(applicationContext), same(configuration), captor.capture(), any(GooglePayIsReadyToPayCallback.class));

        String actualJson = captor.getValue().toJson();
        JSONAssert.assertEquals(
                Fixtures.READY_TO_PAY_REQUEST_WITH_EXISTING_PAYMENT_METHOD, actualJson, false);
    }

    @Test
    public void isReadyToPay_returnsFalseWhenGooglePayIsNotEnabled() {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);

        sut.isReadyToPay(activity, null, readyToPayCallback);
        verify(readyToPayCallback).onResult(false, null);
    }

    @Test
    public void isReadyToPay_whenActivityIsNull_forwardsErrorToCallback() {
        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder().enabled(true))
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.isReadyToPay(null, null, readyToPayCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(readyToPayCallback).onResult(eq(false), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof IllegalArgumentException);
        assertEquals("Context cannot be null.", exception.getMessage());
    }

    // endregion

    // region requestPayment

    @Test
    public void requestPayment_withObserver_launchesWithObserver() {
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

        GooglePayClient sut = new GooglePayClient(null, null, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, baseRequest);

        verify(sut.observer).launch(any(GooglePayIntentData.class));
    }

    @Test
    public void requestPayment_withoutObserver_startsActivityWithDeprecatedAPI() {
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

        GooglePayClient sut = new GooglePayClient(null, null, braintreeClient, internalGooglePayClient);
        sut.requestPayment(activity, baseRequest, requestPaymentCallback);

        verify(activity).startActivityForResult(any(Intent.class), eq(BraintreeRequestCodes.GOOGLE_PAY));
    }

    @Test
    public void requestPayment_withObserver_launchesWithOptionalValues() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(null, null, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, googlePayRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());

        GooglePayIntentData intent = captor.getValue();

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
    public void requestPayment_withoutObserver_startsActivityWithOptionalValues() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(null, null, braintreeClient, internalGooglePayClient);
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
    public void requestPayment_includesATokenizationKeyWhenPresent() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, googlePayRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());
        GooglePayIntentData intent = captor.getValue();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods");
        JSONObject card = allowedPaymentMethods.getJSONObject(0);
        JSONObject tokenizationSpecification = card.getJSONObject("tokenizationSpecification");
        JSONObject cardTokenizationSpecificationParams = tokenizationSpecification.getJSONObject("parameters");
        assertEquals(Fixtures.TOKENIZATION_KEY, cardTokenizationSpecificationParams.get("braintree:clientKey"));
    }

    @Test
    public void requestPayment_doesNotIncludeATokenizationKeyWhenNotPresent() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, googlePayRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());
        GooglePayIntentData intent = captor.getValue();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods");
        JSONObject card = allowedPaymentMethods.getJSONObject(0);
        JSONObject tokenizationSpecification = card.getJSONObject("tokenizationSpecification");
        JSONObject cardTokenizationSpecificationParams = tokenizationSpecification.getJSONObject("parameters");
        assertFalse(cardTokenizationSpecificationParams.has("braintree:clientKey"));
    }

    @Test
    public void requestPayment_sendsAnalyticsEvent() {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.setListener(listener);
        sut.requestPayment(activity, googlePayRequest);

        InOrder order = inOrder(braintreeClient);
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.selected"));
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.started"));
    }

    @Test
    public void requestPayment_postsExceptionWhenTransactionInfoIsNull() {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.setListener(listener);
        sut.requestPayment(activity, googlePayRequest);

        InOrder order = inOrder(braintreeClient);
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.selected"));
        order.verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));
    }

    @Test
    public void requestPayment_whenMerchantNotConfigured_returnsExceptionToFragment() {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(configuration)
                .activityInfo(activityInfo)
                .build();

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.setListener(listener);
        sut.requestPayment(activity, baseRequest);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onGooglePayFailure(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("Google Pay is not enabled for your Braintree account, or Google Play Services are not configured correctly.",
                captor.getValue().getMessage());
    }

    @Test
    public void requestPayment_whenSandbox_setsTestEnvironment() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, baseRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());
        GooglePayIntentData intent = captor.getValue();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getGooglePayEnvironment());
        assertEquals("TEST", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void requestPayment_whenProduction_setsProductionEnvironment() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, baseRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());
        GooglePayIntentData intent = captor.getValue();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_PRODUCTION, intent.getGooglePayEnvironment());
        assertEquals("PRODUCTION", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void requestPayment_withGoogleMerchantId_sendGoogleMerchantId() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, googlePayRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());
        GooglePayIntentData intent = captor.getValue();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals("google-merchant-id-override", paymentDataRequestJson
                .getJSONObject("merchantInfo")
                .getString("merchantId"));
    }

    @Test
    public void requestPayment_withGoogleMerchantName_sendGoogleMerchantName() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, googlePayRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());
        GooglePayIntentData intent = captor.getValue();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals("google-merchant-name-override", paymentDataRequestJson
                .getJSONObject("merchantInfo")
                .getString("merchantName"));
    }

    @Test
    public void requestPayment_whenGooglePayCanProcessPayPal_tokenizationPropertiesIncludePayPal() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, googlePayRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());
        GooglePayIntentData intent = captor.getValue();

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
    public void requestPayment_whenPayPalDisabledByRequest_tokenizationPropertiesLackPayPal() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, googlePayRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());
        GooglePayIntentData intent = captor.getValue();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedPaymentMethods = paymentDataRequestJson
                .getJSONArray("allowedPaymentMethods");

        assertEquals(1, allowedPaymentMethods.length());
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
    }

    @Test
    public void requestPayment_whenPayPalDisabledInConfigurationAndGooglePayHasPayPalClientId_tokenizationPropertiesContainPayPal() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, googlePayRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());
        GooglePayIntentData intent = captor.getValue();

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
    public void requestPayment_usesGooglePayConfigurationClientId() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, googlePayRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());
        GooglePayIntentData intent = captor.getValue();

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
    public void requestPayment_whenGooglePayConfigurationLacksClientId_tokenizationPropertiesLackPayPal() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, googlePayRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());
        GooglePayIntentData intent = captor.getValue();

        PaymentDataRequest paymentDataRequest = intent.getPaymentDataRequest();
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        JSONArray allowedPaymentMethods = paymentDataRequestJson
                .getJSONArray("allowedPaymentMethods");

        assertEquals(1, allowedPaymentMethods.length());
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));

        assertFalse(allowedPaymentMethods.toString().contains("paypal-client-id-for-google-payment"));
    }

    @Test
    public void requestPayment_whenConfigurationContainsElo_addsEloAndEloDebitToAllowedPaymentMethods() throws JSONException {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.requestPayment(activity, googlePayRequest);

        ArgumentCaptor<GooglePayIntentData> captor = ArgumentCaptor.forClass(GooglePayIntentData.class);
        verify(sut.observer).launch(captor.capture());
        GooglePayIntentData intent = captor.getValue();

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
    public void requestPayment_whenRequestIsNull_forwardsExceptionToListener() {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.observer = mock(GooglePayLifecycleObserver.class);
        sut.setListener(listener);
        sut.requestPayment(activity, null);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onGooglePayFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("Cannot pass null GooglePayRequest to requestPayment", exception.getMessage());
    }

    @Test
    public void requestPayment_whenManifestInvalid_forwardsExceptionToListener() {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.setListener(listener);
        sut.requestPayment(activity, baseRequest);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onGooglePayFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("GooglePayActivity was not found in the Android " +
                "manifest, or did not have a theme of R.style.bt_transparent_activity", exception.getMessage());
    }

    // endregion

    // region tokenize

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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.tokenize(pd, activityResultCallback);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(activityResultCallback).onResult(captor.capture(), isNull());

        assertTrue(captor.getValue() instanceof GooglePayCardNonce);
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.tokenize(pd, activityResultCallback);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(activityResultCallback).onResult(captor.capture(), (Exception) isNull());

        assertTrue(captor.getValue() instanceof PayPalAccountNonce);
    }

    // endregion

    // region onGooglePayResult

    @Test
    public void onGooglePayResult_whenPaymentDataExists_returnsResultToListener_andSendsAnalytics() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.setListener(listener);

        String paymentDataJson = Fixtures.RESPONSE_GOOGLE_PAY_CARD;
        PaymentData paymentData = PaymentData.fromJson(paymentDataJson);
        GooglePayResult googlePayResult = new GooglePayResult(paymentData, null);
        sut.onGooglePayResult(googlePayResult);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.authorized"));
        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(listener).onGooglePaySuccess(captor.capture());

        PaymentMethodNonce nonce = captor.getValue();
        JSONObject result = new JSONObject(paymentData.toJson());
        PaymentMethodNonce expectedNonce = GooglePayCardNonce.fromJSON(result);
        assertEquals(nonce.getString(), expectedNonce.getString());
    }

    @Test
    public void onGooglePayResult_whenErrorExists_returnsErrorToListener_andSendsAnalytics() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.setListener(listener);

        Exception error = new Exception("Error");
        GooglePayResult googlePayResult = new GooglePayResult(null, error);
        sut.onGooglePayResult(googlePayResult);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));
        verify(listener).onGooglePayFailure(error);
    }

    @Test
    public void onGooglePayResult_whenUserCanceledErrorExists_returnsErrorToListener_andSendsAnalytics() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.setListener(listener);

        UserCanceledException userCanceledError = new UserCanceledException("User canceled Google Pay.");
        GooglePayResult googlePayResult = new GooglePayResult(null, userCanceledError);
        sut.onGooglePayResult(googlePayResult);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.canceled"));
        verify(listener).onGooglePayFailure(userCanceledError);
    }

    // endregion

    // region onActivityResult

    @Test
    public void onActivityResult_OnCancel_sendsAnalyticsAndReturnsErrorToCallback() {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.onActivityResult(RESULT_CANCELED, new Intent(), activityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.canceled"));

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(activityResultCallback).onResult((PaymentMethodNonce) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertEquals("User canceled Google Pay.", exception.getMessage());
        assertTrue(exception instanceof UserCanceledException);
        assertTrue(((UserCanceledException) exception).isExplicitCancelation());
    }

    @Test
    public void onActivityResult_OnNonOkOrCanceledResult_sendsAnalyticsAndReturnsErrorToCallback() {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.onActivityResult(RESULT_FIRST_USER, new Intent(), activityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.failed"));

        ArgumentCaptor<GooglePayException> captor = ArgumentCaptor.forClass(GooglePayException.class);
        verify(activityResultCallback).onResult((PaymentMethodNonce) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertEquals("An error was encountered during the Google Pay flow. See the status object in this exception for more details.", exception.getMessage());
        assertTrue(exception instanceof BraintreeException);
    }

    @Test
    public void onActivityResult_OnOkResponse_sendsAnalytics() {
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

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);
        sut.onActivityResult(RESULT_OK, new Intent(), activityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(eq("google-payment.authorized"));
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();

        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);

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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();
        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);

        Bundle tokenizationParameters = sut.getTokenizationParameters(configuration, authorization).getParameters();

        assertEquals("braintree", tokenizationParameters.getString("gateway"));
        assertEquals(configuration.getMerchantId(), tokenizationParameters.getString("braintree:merchantId"));
        assertEquals(configuration.getGooglePayAuthorizationFingerprint(),
                tokenizationParameters.getString("braintree:authorizationFingerprint"));
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME, tokenizationParameters.getString("braintree:sdkVersion"));
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();
        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);

        Bundle tokenizationParameters = sut.getTokenizationParameters(configuration, authorization).getParameters();
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();
        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);

        Bundle tokenizationParameters = sut.getTokenizationParameters(configuration, authorization).getParameters();
        assertEquals(Fixtures.TOKENIZATION_KEY, tokenizationParameters.getString("braintree:clientKey"));
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

        GooglePayInternalClient internalGooglePayClient = new MockGooglePayInternalClientBuilder().build();
        GooglePayClient sut = new GooglePayClient(activity, lifecycle, braintreeClient, internalGooglePayClient);

        GooglePayGetTokenizationParametersCallback getTokenizationParametersCallback = mock(GooglePayGetTokenizationParametersCallback.class);
        sut.getTokenizationParameters(getTokenizationParametersCallback);

        verify(getTokenizationParametersCallback).onResult(any(PaymentMethodTokenizationParameters.class), eq(sut.getAllowedCardNetworks(configuration)));
    }

    // endregion
}