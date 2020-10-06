package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PayPalTwoFactorAuthCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalTwoFactorAuthRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.paypal.android.sdk.data.collector.PayPalDataCollector;
import com.paypal.android.sdk.onetouch.core.config.Recipe;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PrepareForTest({PayPalTwoFactorAuthSharedPreferences.class, PayPal.class, Recipe.class, PayPalDataCollector.class})
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "javax.crypto.*" })
public class PayPalTwoFactorAuthUnitTest {
    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private static final String CREATE_PAYMENT_RESOURCE_PATH = "/v1/paypal_hermes/create_payment_resource";
    private static final String CREATE_PAYPAL_ACCOUNT_PATH = "/v1/payment_methods/paypal_accounts";

    private static final String CREATE_PAYMENT_RESOURCE_RESPONSE = stringFromFixture(
            "payment_methods/hermes_payment_resource/response_with_authenticate_url.json");
    private static final String CREATE_PAYPAL_ACCOUNT_RESPONSE = stringFromFixture(
            "payment_methods/paypal_account_response.json");
    
    private MockFragmentBuilder mMockFragmentBuilder;
    private PayPalTwoFactorAuthRequest payPalTwoFactorAuthRequest;
    private PayPalTwoFactorAuthCallback emptyPayPalTwoFactorAuthCallback;

    @Before
    public void setup() throws Exception {
        spy(PayPal.class);
        doReturn(true).when(PayPal.class, "isManifestValid", any(Context.class));

        spy(Recipe.class);
        doReturn(true).when(Recipe.class, "isValidBrowserTarget", any(Context.class), anyString(), anyString());

        Authorization authorization = mock(Authorization.class);
        when(authorization.getBearer()).thenReturn("authorization");
        when(authorization.toString()).thenReturn("authorization");

        Configuration configuration = new TestConfigurationBuilder()
                .withAnalytics()
                .paypal(new TestConfigurationBuilder.TestPayPalConfigurationBuilder(true)
                        .environment("offline")
                        .billingAgreementsEnabled(false))
                .buildConfiguration();

        mMockFragmentBuilder = new MockFragmentBuilder()
                .authorization(authorization)
                .configuration(configuration);

        payPalTwoFactorAuthRequest = new PayPalTwoFactorAuthRequest()
                .nonce("fake-nonce")
                .amount("1000")
                .currencyCode("INR");

        emptyPayPalTwoFactorAuthCallback = new PayPalTwoFactorAuthCallback() {
            @Override
            public void onLookupResult(@NonNull PaymentMethodNonce nonce) {}

            @Override
            public void onLookupFailure(@NonNull Exception exception) {}
        };
    }

    @Test
    public void performTwoFactorLookup_postsExpectedRequestToCreatePaymentResource() throws JSONException {
        BraintreeFragment mockBraintreeFragment = mMockFragmentBuilder.build();
        BraintreeHttpClient mockHttpClient = mock(BraintreeHttpClient.class);
        when(mockBraintreeFragment.getHttpClient()).thenReturn(mockHttpClient);

        PayPalTwoFactorAuth.performTwoFactorLookup(mockBraintreeFragment, payPalTwoFactorAuthRequest, emptyPayPalTwoFactorAuthCallback);

        String expectedRequest = "{\n" +
                "  \"authorization_fingerprint\": \"authorization\",\n" +
                "  \"amount\": \"1000\",\n" +
                "  \"currency_iso_code\": \"INR\",\n" +
                "  \"return_url\": \"com.braintreepayments.api.braintree:\\/\\/success\",\n" +
                "  \"cancel_url\": \"com.braintreepayments.api.braintree:\\/\\/cancel\",\n" +
                "  \"vault_initiated_checkout_payment_method_token\": \"fake-nonce\"\n" +
                "}";

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockHttpClient).post(eq(CREATE_PAYMENT_RESOURCE_PATH), captor.capture(), any(HttpResponseCallback.class));
        JSONAssert.assertEquals(expectedRequest, captor.getValue(), true);
    }

    @Test
    public void performTwoFactorLookup_postsExpectedRequestToCreatePayPalAccount() throws JSONException {
        BraintreeFragment mockBraintreeFragment = mMockFragmentBuilder.build();
        BraintreeHttpClient mockHttpClient = mock(BraintreeHttpClient.class);
        when(mockBraintreeFragment.getHttpClient()).thenReturn(mockHttpClient);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((HttpResponseCallback) invocation.getArguments()[2]).success(CREATE_PAYMENT_RESOURCE_RESPONSE);
                return null;
            }
        }).when(mockHttpClient).post(eq(CREATE_PAYMENT_RESOURCE_PATH), anyString(), any(HttpResponseCallback.class));

        mockStatic(PayPalDataCollector.class);
        when(PayPalDataCollector.getClientMetadataId(any(Context.class))).thenReturn("fake-metadata-id");

        PayPalTwoFactorAuth.performTwoFactorLookup(mockBraintreeFragment, payPalTwoFactorAuthRequest, emptyPayPalTwoFactorAuthCallback);

        String expectedRequest = "{\n" +
                "  \"paypal_account\": {\n" +
                "    \"correlation_id\" : \"fake-metadata-id\",\n" +
                "    \"payment_token\": \"fake-token\",\n" +
                "    \"options\": {\n" +
                "      \"sca_authentication_complete\": false\n" +
                "    }\n" +
                "  },\n" +
                "  \"authorization_fingerprint\": \"authorization\"\n" +
                "}";

        verify(mockHttpClient).post(eq(CREATE_PAYMENT_RESOURCE_PATH), anyString(), any(HttpResponseCallback.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockHttpClient).post(eq(CREATE_PAYPAL_ACCOUNT_PATH), captor.capture(), any(HttpResponseCallback.class));
        JSONAssert.assertEquals(expectedRequest, captor.getValue(), true);
    }

    @Test
    public void performTwoFactorLookup_successfullyReturnsPayPalAccountNonce() {
        BraintreeFragment mockBraintreeFragment = mMockFragmentBuilder.build();
        BraintreeHttpClient mockHttpClient = mock(BraintreeHttpClient.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((HttpResponseCallback) invocation.getArguments()[2]).success(CREATE_PAYMENT_RESOURCE_RESPONSE);
                return null;
            }
        }).when(mockHttpClient).post(eq(CREATE_PAYMENT_RESOURCE_PATH), anyString(), any(HttpResponseCallback.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((HttpResponseCallback) invocation.getArguments()[2]).success(CREATE_PAYPAL_ACCOUNT_RESPONSE);
                return null;
            }
        }).when(mockHttpClient).post(eq(CREATE_PAYPAL_ACCOUNT_PATH), anyString(), any(HttpResponseCallback.class));

        when(mockBraintreeFragment.getHttpClient()).thenReturn(mockHttpClient);

        PayPalTwoFactorAuthCallback mockCallback = mock(PayPalTwoFactorAuthCallback.class);
        PayPalTwoFactorAuth.performTwoFactorLookup(mockBraintreeFragment, payPalTwoFactorAuthRequest, mockCallback);
        verify(mockBraintreeFragment).sendAnalyticsEvent("paypal-two-factor.perform-two-factor-lookup.started");

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(mockCallback).onLookupResult(captor.capture());

        PayPalAccountNonce receivedNonce = (PayPalAccountNonce) captor.getValue();

        assertNotNull(receivedNonce.getNonce());
        assertNotNull(receivedNonce.getAuthenticateUrl());
        assertNotNull(receivedNonce.getBillingAddress());
        assertNotNull(receivedNonce.getShippingAddress());
        assertNotNull(receivedNonce.getEmail());
    }

    @Test
    public void performTwoFactorLookup_throwsException_whenManifestIsNotValid() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPalTwoFactorAuthCallback callback = mock(PayPalTwoFactorAuthCallback.class);

        mockStatic(PayPal.class);
        when(PayPal.isManifestValid(any(BraintreeFragment.class))).thenReturn(false);

        PayPalTwoFactorAuth.performTwoFactorLookup(fragment, payPalTwoFactorAuthRequest, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("BraintreeBrowserSwitchActivity missing, " +
                "incorrectly configured in AndroidManifest.xml or another app defines the same browser " +
                "switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                "for the correct configuration", captor.getValue().getMessage());
    }

    @Test
    public void performTwoFactorLookup_throwsException_whenPayPalIsNotEnabled() {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();

        BraintreeFragment fragment = mMockFragmentBuilder
                .configuration(configuration)
                .build();

        PayPalTwoFactorAuthCallback callback = mock(PayPalTwoFactorAuthCallback.class);

        PayPalTwoFactorAuth.performTwoFactorLookup(fragment, payPalTwoFactorAuthRequest, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals( "PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.", captor.getValue().getMessage());
    }

    @Test
    public void continueTwoFactorAuthentication_returnsNonceWhenAuthenticateUrlIsNull() {
        BraintreeFragment braintreeFragment = mMockFragmentBuilder.build();

        mockStatic(PayPalTwoFactorAuthSharedPreferences.class);
        PayPalAccountNonce nonce = mock(PayPalAccountNonce.class);
        when(nonce.getAuthenticateUrl()).thenReturn(null);

        PayPalTwoFactorAuth.continueTwoFactorAuthentication(braintreeFragment, nonce);
        verify(braintreeFragment).sendAnalyticsEvent("paypal-two-factor.continue-two-factor-authentication.started");
        verify(braintreeFragment).sendAnalyticsEvent("paypal-two-factor.continue-two-factor-authentication.no-two-factor-required");
        verify(braintreeFragment).postCallback(nonce);
    }

    @Test
    public void continueTwoFactorAuthentication_startsBrowserSwitchWhenAuthenticateUrlIsPresent() {
        BraintreeFragment braintreeFragment = mMockFragmentBuilder.build();

        mockStatic(PayPalTwoFactorAuthSharedPreferences.class);
        PayPalAccountNonce nonce = mock(PayPalAccountNonce.class);
        when(nonce.getAuthenticateUrl()).thenReturn("auth-url");

        PayPalTwoFactorAuth.continueTwoFactorAuthentication(braintreeFragment, nonce);
        verify(braintreeFragment).sendAnalyticsEvent("paypal-two-factor.continue-two-factor-authentication.started");
        verify(braintreeFragment).sendAnalyticsEvent("paypal-two-factor.browser-switch.started");
        verify(braintreeFragment).browserSwitch(BraintreeRequestCodes.PAYPAL_TWO_FACTOR_AUTH, "auth-url");
    }

    @Test
    public void onActivityResult_returnsNonceOnSuccessfulBrowserSwitch() {
        BraintreeFragment braintreeFragment = mMockFragmentBuilder.build();

        Intent intent = mock(Intent.class);
        Uri uri = mock(Uri.class);
        PayPalAccountNonce nonce = mock(PayPalAccountNonce.class);

        when(uri.getHost()).thenReturn("success");
        when(intent.getData()).thenReturn(uri);
        mockStatic(PayPalTwoFactorAuthSharedPreferences.class);
        when(PayPalTwoFactorAuthSharedPreferences.getPersistedPayPalAccountNonce(braintreeFragment)).thenReturn(nonce);

        PayPalTwoFactorAuth.onActivityResult(braintreeFragment, AppCompatActivity.RESULT_OK, intent);

        verify(braintreeFragment).sendAnalyticsEvent("paypal-two-factor.browser-switch.succeeded");
        verify(braintreeFragment).postCallback(nonce);
    }

    @Test
    public void onActivityResult_callsCancelCallbackWhenBrowserSwitchIsCancelled() {
        BraintreeFragment braintreeFragment = mMockFragmentBuilder.build();

        Intent intent = mock(Intent.class);
        Uri uri = mock(Uri.class);
        PayPalAccountNonce nonce = mock(PayPalAccountNonce.class);

        when(uri.getHost()).thenReturn("cancel");
        when(intent.getData()).thenReturn(uri);
        mockStatic(PayPalTwoFactorAuthSharedPreferences.class);
        when(PayPalTwoFactorAuthSharedPreferences.getPersistedPayPalAccountNonce(braintreeFragment)).thenReturn(nonce);

        PayPalTwoFactorAuth.onActivityResult(braintreeFragment, AppCompatActivity.RESULT_OK, intent);

        verify(braintreeFragment).sendAnalyticsEvent("paypal-two-factor.browser-switch.canceled");
        verify(braintreeFragment).postCancelCallback(BraintreeRequestCodes.PAYPAL_TWO_FACTOR_AUTH);
    }

    @Test
    public void onActivityResult_returnsExceptionWhenBrowserSwitchFails() {
        BraintreeFragment braintreeFragment = mMockFragmentBuilder.build();

        Intent intent = mock(Intent.class);
        Uri uri = mock(Uri.class);
        PayPalAccountNonce nonce = mock(PayPalAccountNonce.class);

        when(uri.getHost()).thenReturn("about:none");
        when(intent.getData()).thenReturn(uri);
        mockStatic(PayPalTwoFactorAuthSharedPreferences.class);
        when(PayPalTwoFactorAuthSharedPreferences.getPersistedPayPalAccountNonce(braintreeFragment)).thenReturn(nonce);

        PayPalTwoFactorAuth.onActivityResult(braintreeFragment, AppCompatActivity.RESULT_OK, intent);
        verify(braintreeFragment).sendAnalyticsEvent("paypal-two-factor.browser-switch.failed");

        ArgumentCaptor<BraintreeException> captor = ArgumentCaptor.forClass(BraintreeException.class);
        verify(braintreeFragment).postCallback(captor.capture());
        BraintreeException exception = captor.getValue();
        assertEquals(exception.getMessage(), "Host path unknown: about:none");
    }

}
