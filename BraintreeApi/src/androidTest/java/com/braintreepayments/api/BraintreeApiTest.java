package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.internal.HttpResponse;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.AndroidPayConfiguration;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.TestClientTokenBuilder;
import com.google.android.gms.wallet.Cart;
import com.google.gson.Gson;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;

import org.json.JSONException;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicInteger;

import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BraintreeApiTest extends AndroidTestCase {

    private Context mContext;

    @Override
    public void setUp() {
        mContext = getContext();
        TestUtils.setUp(mContext);
    }

    public void testThrowsConfigurationExceptionOnBadPayPalConfiguration()
            throws ErrorWithResponse, BraintreeException {
        Configuration configuration = mock(Configuration.class);
        BraintreeApi braintreeApi = new BraintreeApi(mContext, mock(ClientToken.class),
                configuration, mock(HttpRequest.class));
        boolean exceptionHappened = false;

        try {
            braintreeApi.finishPayWithPayPal(null, PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID,
                    new Intent());
            fail("Configuration exception not thrown");
        } catch (ConfigurationException e) {
            exceptionHappened = true;
        }

        assertTrue("Expected ConfigurationException was not thrown", exceptionHappened);
    }

    public void testFinishPayWithVenmoReturnsANonce() {
        BraintreeApi braintreeApi = new BraintreeApi(mContext, new TestClientTokenBuilder().build());
        Intent intent = new Intent().putExtra(AppSwitch.EXTRA_PAYMENT_METHOD_NONCE,
                "payment method nonce");

        assertEquals("payment method nonce", braintreeApi.finishPayWithVenmo(Activity.RESULT_OK,
                intent));
    }

    public void testPayWithVenmoReturnsNullIfResultCodeNotOK() {
        BraintreeApi braintreeApi = new BraintreeApi(mContext, new TestClientTokenBuilder().build());

        assertNull(braintreeApi.finishPayWithVenmo(Activity.RESULT_CANCELED, new Intent()));
    }

    public void testGetPaymentMethodReturnsPaymentMethodFromNonce()
            throws ErrorWithResponse, BraintreeException, JSONException {
        BraintreeApi braintreeApi = new BraintreeApi(mContext, new TestClientTokenBuilder().build());
        Card card = braintreeApi.create(
                new CardBuilder().cardNumber(VISA).expirationDate("06/20"));

        Card cardFromShowNonce = (Card) braintreeApi.getPaymentMethod(card.getNonce());

        assertEquals(card.getLastTwo(), cardFromShowNonce.getLastTwo());
        assertEquals(card.getTypeLabel(), cardFromShowNonce.getTypeLabel());
    }

    public void testSendAnalyticsEventSendsAnalyticsIfEnabled()
            throws BraintreeException, ErrorWithResponse {
        HttpRequest httpRequest = mock(HttpRequest.class);
        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.getUrl()).thenReturn("analytics_url");
        Configuration configuration = mock(Configuration.class);
        when(configuration.isAnalyticsEnabled()).thenReturn(true);
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        BraintreeApi braintreeApi = new BraintreeApi(mContext, mock(ClientToken.class),
                configuration, httpRequest);

        braintreeApi.sendAnalyticsEvent("very.important.analytics-payload", "TEST");

        verify(httpRequest, times(1)).post(matches("analytics_url"),
                contains("very.important.analytics-payload"));
    }

    public void testSendAnalyticsEventNoopsIfDisabled() throws BraintreeException,
            ErrorWithResponse {
        HttpRequest httpRequest = mock(HttpRequest.class);
        Configuration configuration = mock(Configuration.class);
        when(configuration.isAnalyticsEnabled()).thenReturn(false);
        BraintreeApi braintreeApi = new BraintreeApi(mContext, mock(ClientToken.class),
                configuration, httpRequest);

        braintreeApi.sendAnalyticsEvent("event", "TEST");

        verify(httpRequest, never()).post(anyString(), anyString());
    }

    public void testAnalyticsEventsAreSentToServer() throws ErrorWithResponse, BraintreeException {
        final AtomicInteger requestCount = new AtomicInteger(0);
        final AtomicInteger responseCode = new AtomicInteger(0);

        String clientTokenString = new TestClientTokenBuilder().build();
        ClientToken clientToken = ClientToken.fromString(clientTokenString);
        Configuration configuration = Configuration.fromJson(clientTokenString);
        HttpRequest request = new HttpRequest(clientToken.getAuthorizationFingerprint()) {
            @Override
            public HttpResponse post (String url, String params)
                    throws BraintreeException, ErrorWithResponse {
                requestCount.incrementAndGet();
                HttpResponse response = super.post(url, params);
                responseCode.set(response.getResponseCode());
                return response;
            }
        };
        request.setBaseUrl(configuration.getClientApiUrl());
        BraintreeApi braintreeApi = new BraintreeApi(mContext, clientToken, configuration, request);
        braintreeApi.setup();

        braintreeApi.sendAnalyticsEvent("event", "TEST");
        assertEquals(1, requestCount.get());
        assertEquals(200, responseCode.get());

        braintreeApi.sendAnalyticsEvent("another-event", "TEST");
        assertEquals(2, requestCount.get());
        assertEquals(200, responseCode.get());
    }

    public void testGetConfigurationReturnsConfigurationAsAString() {
        Configuration configuration = Configuration.fromJson(new TestClientTokenBuilder().build());
        BraintreeApi braintreeApi = new BraintreeApi(mContext, mock(ClientToken.class), configuration,
                mock(HttpRequest.class));

        String configurationString = braintreeApi.getConfigurationString();

        assertEquals(new Gson().toJson(configuration), configurationString);
    }

    public void testGetConfigurationReturnsNullIfConfigurationIsNull() {
        BraintreeApi braintreeApi = new BraintreeApi(mContext, mock(ClientToken.class), null,
                mock(HttpRequest.class));

        String configurationString = braintreeApi.getConfigurationString();

        assertNull(configurationString);
    }

    public void testGetAndroidPayPaymentMethodTokenizationParametersReturnsParameters() {
        AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
        when(androidPayConfiguration.getGoogleAuthorizationFingerprint()).thenReturn("google-auth-fingerprint");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("android-pay-merchant-id");
        when(configuration.getAndroidPay()).thenReturn(androidPayConfiguration);

        BraintreeApi braintreeApi = new BraintreeApi(mContext, mock(ClientToken.class), configuration, null);
        Bundle tokenizationParameters = braintreeApi.getAndroidPayTokenizationParameters().getParameters();

        assertEquals("braintree", tokenizationParameters.getString("gateway"));
        assertEquals(configuration.getMerchantId(), tokenizationParameters.getString(
                "braintree:merchantId"));
        assertEquals(androidPayConfiguration.getGoogleAuthorizationFingerprint(),
                tokenizationParameters.getString("braintree:authorizationFingerprint"));
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME, tokenizationParameters.getString(
                "braintree:sdkVersion"));
    }

    public void testStartPayWithAndroidPayIncludesAllParametersForBillingAgreement() throws InvalidArgumentException {
        ArgumentCaptor<Intent> launchIntentCaptor = ArgumentCaptor.forClass(Intent.class);
        Activity mockActivity = mock(Activity.class);

        String clientTokenString = new Gson().toJson(
                ClientToken.fromString(stringFromFixture(mContext, "client_token.json")));
        String configurationString = new Gson().toJson(
                Configuration.fromJson(stringFromFixture(mContext, "configuration_with_android_pay.json")));
        BraintreeApi braintreeApi = new BraintreeApi(mContext,
                ClientToken.fromString(clientTokenString), Configuration.fromJson(configurationString),
                null);

        braintreeApi.startPayWithAndroidPay(mockActivity, 2737, null, true, true, true);

        verify(mockActivity).startActivityForResult(launchIntentCaptor.capture(), anyInt());
        Intent launchIntent = launchIntentCaptor.getValue();
        assertEquals(clientTokenString, launchIntent.getStringExtra(AndroidPayActivity.EXTRA_CLIENT_TOKEN));
        assertEquals(configurationString, launchIntent.getStringExtra(AndroidPayActivity.EXTRA_CONFIGURATION));
        assertEquals(null, launchIntent.getParcelableExtra(AndroidPayActivity.EXTRA_CART));
        assertEquals(true, launchIntent.getBooleanExtra(AndroidPayActivity.EXTRA_IS_BILLING_AGREEMENT, false));
        assertEquals(true, launchIntent.getBooleanExtra(AndroidPayActivity.EXTRA_SHIPPING_ADDRESS_REQUIRED, false));
        assertEquals(true, launchIntent.getBooleanExtra(AndroidPayActivity.EXTRA_PHONE_NUMBER_REQUIRED, false));
    }

    public void testStartPayWithAndroidPayIncludesAllParametersForNonBillingAgreement() throws InvalidArgumentException {
        ArgumentCaptor<Intent> launchIntentCaptor = ArgumentCaptor.forClass(Intent.class);
        Activity mockActivity = mock(Activity.class);

        String clientTokenString = new Gson().toJson(
                ClientToken.fromString(stringFromFixture(mContext, "client_token.json")));
        String configurationString = new Gson().toJson(
                Configuration.fromJson(stringFromFixture(mContext, "configuration_with_android_pay.json")));
        Cart cart = Cart.newBuilder().build();
        BraintreeApi braintreeApi = new BraintreeApi(mContext,
                ClientToken.fromString(clientTokenString), Configuration.fromJson(configurationString),
                null);

        braintreeApi.startPayWithAndroidPay(mockActivity, 2737, cart, false, true, true);

        verify(mockActivity).startActivityForResult(launchIntentCaptor.capture(), anyInt());
        Intent launchIntent = launchIntentCaptor.getValue();
        assertEquals(clientTokenString, launchIntent.getStringExtra(AndroidPayActivity.EXTRA_CLIENT_TOKEN));
        assertEquals(configurationString, launchIntent.getStringExtra(AndroidPayActivity.EXTRA_CONFIGURATION));
        assertEquals(cart, launchIntent.getParcelableExtra(AndroidPayActivity.EXTRA_CART));
        assertEquals(false, launchIntent.getBooleanExtra(AndroidPayActivity.EXTRA_IS_BILLING_AGREEMENT, false));
        assertEquals(true, launchIntent.getBooleanExtra(AndroidPayActivity.EXTRA_SHIPPING_ADDRESS_REQUIRED, false));
        assertEquals(true, launchIntent.getBooleanExtra(AndroidPayActivity.EXTRA_PHONE_NUMBER_REQUIRED, false));
    }
}
