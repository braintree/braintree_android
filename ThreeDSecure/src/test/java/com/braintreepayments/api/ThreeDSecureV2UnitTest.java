package com.braintreepayments.api;

import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.MockBraintreeClientBuilder;
import com.braintreepayments.MockTokenizationClientBuilder;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.helpers.MockCardinalClientBuilder;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.interfaces.ThreeDSecurePrepareLookupCallback;
import com.braintreepayments.api.internal.ThreeDSecureV1BrowserSwitchHelper;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.testutils.Fixtures;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;
import com.cardinalcommerce.shared.userinterfaces.TextBoxCustomization;
import com.cardinalcommerce.shared.userinterfaces.UiCustomization;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.models.BraintreeRequestCodes.THREE_D_SECURE;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2UnitTest {

    private FragmentActivity activity;
    private ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper;

    private ThreeDSecureInitializeChallengeCallback initializeChallengeListener;

    private Configuration threeDSecureEnabledConfig;
    private ThreeDSecureRequest mBasicRequest;

    @Before
    public void setup() {
        activity = mock(FragmentActivity.class);
        initializeChallengeListener = mock(ThreeDSecureInitializeChallengeCallback.class);
        browserSwitchHelper = mock(ThreeDSecureV1BrowserSwitchHelper.class);

        threeDSecureEnabledConfig = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .cardinalAuthenticationJWT("cardinal_authentication_jwt")
                .buildConfiguration();

        TextBoxCustomization textBoxCustomization = new TextBoxCustomization();
        textBoxCustomization.setBorderWidth(12);

        UiCustomization uiCustomization = new UiCustomization();
        uiCustomization.setTextBoxCustomization(textBoxCustomization);

        mBasicRequest = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00")
                .versionRequested(ThreeDSecureRequest.VERSION_2)
                .uiCustomization(uiCustomization);
    }

    @Test
    public void prepareLookup_returnsValidLookupJSONString() throws InvalidArgumentException, JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("fake-df")
                .build();

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, mBasicRequest, callback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(callback).onResult(same(mBasicRequest), captor.capture(), (Exception) isNull());

        String clientData = captor.getValue();
        JSONObject lookup = new JSONObject(clientData);
        assertEquals("encoded_auth_fingerprint", lookup.getString("authorizationFingerprint"));
        assertEquals(lookup.getString("braintreeLibraryVersion"), "Android-" + BuildConfig.VERSION_NAME);
        assertEquals(lookup.getString("dfReferenceId"), "fake-df");
        assertEquals(lookup.getString("nonce"), "a-nonce");

        JSONObject clientMetaData = lookup.getJSONObject("clientMetadata");
        assertEquals(clientMetaData.getString("requestedThreeDSecureVersion"), "2");
        assertEquals(clientMetaData.getString("sdkVersion"), "Android/" + BuildConfig.VERSION_NAME);
    }

    @Test
    public void prepareLookup_returnsValidLookupJSONString_whenCardinalSetupFails() throws InvalidArgumentException, JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .error(new Exception("cardinal error"))
                .build();

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, mBasicRequest, callback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(callback).onResult(same(mBasicRequest), captor.capture(), (Exception) isNull());

        String clientData = captor.getValue();
        JSONObject lookup = new JSONObject(clientData);
        assertEquals("encoded_auth_fingerprint", lookup.getString("authorizationFingerprint"));
        assertEquals(lookup.getString("braintreeLibraryVersion"), "Android-" + BuildConfig.VERSION_NAME);
        assertEquals(lookup.getString("nonce"), "a-nonce");
        assertFalse(lookup.has("dfReferenceId"));

        JSONObject clientMetaData = lookup.getJSONObject("clientMetadata");
        assertEquals(clientMetaData.getString("requestedThreeDSecureVersion"), "2");
        assertEquals(clientMetaData.getString("sdkVersion"), "Android/" + BuildConfig.VERSION_NAME);
    }

    @Test
    public void prepareLookup_initializesCardinal() throws InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("fake-df")
                .build();

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, mBasicRequest, callback);

        verify(cardinalClient).initialize(same(activity), same(threeDSecureEnabledConfig), same(mBasicRequest), any(CardinalInitializeCallback.class));
    }

    @Test
    public void prepareLookup_withoutCardinalJWT_postsException() throws Exception {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .buildConfiguration();

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(configuration)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, mBasicRequest, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((ThreeDSecureRequest) isNull(), (String) isNull(), captor.capture());

        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals(captor.getValue().getMessage(), "Merchant is not configured for 3DS 2.0. " +
                "Please contact Braintree Support for assistance.");
    }

    @Test
    public void performVerification_withCardBuilder_tokenizesAndPerformsVerification() throws InvalidArgumentException {
        CardNonce cardNonce = mock(CardNonce.class);
        when(cardNonce.getNonce()).thenReturn("card-nonce");

        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("fake-df")
                .build();

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder()
                .successNonce(cardNonce)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        CardBuilder cardBuilder = new CardBuilder();
        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .amount("10");

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecureVerificationCallback callback = mock(ThreeDSecureVerificationCallback.class);
        sut.performVerification(activity, cardBuilder, request, callback);

        verify(tokenizationClient).tokenize(same(activity), same(cardBuilder), any(PaymentMethodNonceCallback.class));
    }

    @Test
    public void performVerification_initializesCardinal() throws InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("df-reference-id")
                .build();

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, mBasicRequest, mock(ThreeDSecureVerificationCallback.class));

        verify(cardinalClient).initialize(same(activity), same(threeDSecureEnabledConfig), same(mBasicRequest), any(CardinalInitializeCallback.class));
    }

    @Test
    public void performVerification_whenCardinalSetupCompleted_sendsAnalyticEvent() throws InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("df-reference-id")
                .build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, mBasicRequest, mock(ThreeDSecureVerificationCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-completed");
    }

    @Test
    public void performVerification_whenCardinalSetupFailed_sendsAnalyticEvent() throws InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .error(new Exception("cardinal error"))
                .build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, mBasicRequest, mock(ThreeDSecureVerificationCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-failed");
    }

    @Test
    public void performVerification_whenAuthenticatingWithCardinal_sendsAnalyticsEvent() throws InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, mBasicRequest, mock(ThreeDSecureVerificationCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.started");
    }

    @Test
    public void performVerification_whenChallengeIsPresented_sendsAnalyticsEvent() throws InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);
        when(browserSwitchHelper.getUrl(anyString(), anyString(), any(ThreeDSecureRequest.class), any(ThreeDSecureLookup.class))).thenReturn("https://example.com");

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, mBasicRequest, mock(ThreeDSecureVerificationCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.challenge-presented.true");
    }

    @Test
    public void performVerification_whenChallengeIsNotPresented_sendsAnalyticsEvent() throws InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, mBasicRequest, mock(ThreeDSecureVerificationCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.challenge-presented.false");
    }

    @Test
    public void performVerification_when3DSVersionIsVersion2_sendsAnalyticsEvent() throws InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, mBasicRequest, mock(ThreeDSecureVerificationCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.3ds-version.2.1.0");
    }

    @Test
    public void performVerification_withoutCardinalJWT_postsException() throws Exception {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(configuration)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecureVerificationCallback callback = mock(ThreeDSecureVerificationCallback.class);
        sut.performVerification(activity, mBasicRequest, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((PaymentMethodNonce) isNull(), captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals(captor.getValue().getMessage(), "Merchant is not configured for 3DS 2.0. " +
                "Please contact Braintree Support for assistance.");
    }

    @Test
    public void initializeChallengeWithLookupResponse_postsExceptionForBadJSON() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        sut.initializeChallengeWithLookupResponse(activity, "{bad:}", initializeChallengeListener);
        verify(initializeChallengeListener).onResult((PaymentMethodNonce) isNull(), any(JSONException.class));
    }

    @Test
    public void authenticateCardinalJWT_whenSuccess_sendsAnalyticsEvent() throws JSONException, InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.authenticateCardinalJWT(activity, threeDSecureLookup, "jwt", mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.started");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.succeeded");
    }

    @Test
    public void authenticateCardinalJWT_whenSuccess_returnsThreeDSecureCardNonce() throws JSONException, InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.authenticateCardinalJWT(activity, threeDSecureLookup, "jwt", callback);

        ArgumentCaptor<CardNonce> captor = ArgumentCaptor.forClass(CardNonce.class);
        verify(callback).onResult(captor.capture(), (Exception) isNull());

        CardNonce cardNonce = captor.getValue();
        assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertEquals("12345678-1234-1234-1234-123456789012", cardNonce.getNonce());
    }

    @Test
    public void authenticateCardinalJWT_whenCustomerFailsAuthentication_sendsAnalyticsEvent() throws JSONException, InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE_WITH_ERROR)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.authenticateCardinalJWT(activity, threeDSecureLookup, "jwt", callback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.started");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce");
    }

    @Test
    public void authenticateCardinalJWT_whenCustomerFailsAuthentication_returnsLookupCardNonce() throws JSONException, InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        String authResponseJson = Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE_WITH_ERROR;
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTSuccessfulResponse(authResponseJson)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(
                Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE_WITHOUT_LIABILITY_WITH_LIABILITY_SHIFT_POSSIBLE);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.authenticateCardinalJWT(activity, threeDSecureLookup, "jwt", callback);

        ArgumentCaptor<CardNonce> captor = ArgumentCaptor.forClass(CardNonce.class);
        verify(callback).onResult(captor.capture(), (Exception) isNull());

        CardNonce cardNonce = captor.getValue();

        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertEquals("123456-12345-12345-a-adfa", cardNonce.getNonce());
        assertEquals("Failed to authenticate, please try a different form of payment.", cardNonce.getThreeDSecureInfo().getErrorMessage());
        assertEquals(authResponseJson, cardNonce.getThreeDSecureInfo().getThreeDSecureAuthenticationResponse().getErrors());
    }

    @Test
    public void authenticateCardinalJWT_whenExceptionOccurs_sendsAnalyticsEvent() throws JSONException, InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTErrorResponse(new BraintreeException("an error occurred!"))
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.authenticateCardinalJWT(activity, threeDSecureLookup, "jwt", callback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.started");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.errored");
    }

    @Test
    public void authenticateCardinalJWT_whenExceptionOccurs_returnsException() throws JSONException, InvalidArgumentException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTErrorResponse(new BraintreeException("an error occurred!"))
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.authenticateCardinalJWT(activity, threeDSecureLookup, "jwt", callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((PaymentMethodNonce) isNull(), captor.capture());

        assertEquals("an error occurred!", captor.getValue().getMessage());
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationReportsSuccess_sendsAnalyticsEvent() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);
        data.putExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        sut.onActivityResult(activity, RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.cardinal-sdk.action-code.success");
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationReportsNoAction_sendsAnalyticsEvent() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.NOACTION);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);
        data.putExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        sut.onActivityResult(activity, RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.cardinal-sdk.action-code.noaction");
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationReportsFailure_sendsAnalyticsEvent() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.FAILURE);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);
        data.putExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        sut.onActivityResult(activity, RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.cardinal-sdk.action-code.failure");
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationIsCanceled_sendsAnalyticsEvent() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.CANCEL);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);

        sut.onActivityResult(activity, RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.canceled");
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationErrors_sendsAnalyticsEvent() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.ERROR);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);

        sut.onActivityResult(activity, RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.failed");
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationTimeout_sendsAnalyticsEvent() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.TIMEOUT);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);

        sut.onActivityResult(activity, RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.failed");
    }
}
