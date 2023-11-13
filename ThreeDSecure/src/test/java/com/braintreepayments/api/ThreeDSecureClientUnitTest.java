package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.fragment.app.FragmentActivity;

import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureClientUnitTest {

    private FragmentActivity activity;
    private ThreeDSecureAPI threeDSecureAPI;

    private ThreeDSecureResultCallback threeDSecureResultCallback;

    private Configuration threeDSecureEnabledConfig;

    ThreeDSecureRequest basicRequest;
    ThreeDSecureResult threeDSecureResult;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        threeDSecureResultCallback = mock(ThreeDSecureResultCallback.class);
        threeDSecureAPI = mock(ThreeDSecureAPI.class);

        threeDSecureEnabledConfig = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .cardinalAuthenticationJWT("cardinal-jwt")
                .buildConfiguration();

        basicRequest = new ThreeDSecureRequest();
        basicRequest.setNonce("a-nonce");
        basicRequest.setAmount("amount");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        basicRequest.setBillingAddress(billingAddress);

        threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
    }

    // region prepareLookup

    @Test
    public void prepareLookup_returnsValidLookupJSONString()
            throws JSONException, BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("fake-df")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, basicRequest, callback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(callback).onResult(same(basicRequest), captor.capture(), isNull());

        String clientData = captor.getValue();
        JSONObject lookup = new JSONObject(clientData);
        Assert.assertEquals("encoded_auth_fingerprint",
                lookup.getString("authorizationFingerprint"));
        Assert.assertEquals(lookup.getString("braintreeLibraryVersion"),
                "Android-" + BuildConfig.VERSION_NAME);
        Assert.assertEquals(lookup.getString("dfReferenceId"), "fake-df");
        Assert.assertEquals(lookup.getString("nonce"), "a-nonce");

        JSONObject clientMetaData = lookup.getJSONObject("clientMetadata");
        Assert.assertEquals(clientMetaData.getString("requestedThreeDSecureVersion"), "2");
        Assert.assertEquals(clientMetaData.getString("sdkVersion"),
                "Android/" + BuildConfig.VERSION_NAME);
    }

    @Test
    public void prepareLookup_returnsValidLookupJSONString_whenCardinalSetupFails()
            throws JSONException, BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .error(new Exception("cardinal error"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, basicRequest, callback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(callback).onResult(same(basicRequest), captor.capture(), (Exception) isNull());

        String clientData = captor.getValue();
        JSONObject lookup = new JSONObject(clientData);
        Assert.assertEquals("encoded_auth_fingerprint",
                lookup.getString("authorizationFingerprint"));
        Assert.assertEquals(lookup.getString("braintreeLibraryVersion"),
                "Android-" + BuildConfig.VERSION_NAME);
        Assert.assertEquals(lookup.getString("nonce"), "a-nonce");
        assertFalse(lookup.has("dfReferenceId"));

        JSONObject clientMetaData = lookup.getJSONObject("clientMetadata");
        Assert.assertEquals(clientMetaData.getString("requestedThreeDSecureVersion"), "2");
        Assert.assertEquals(clientMetaData.getString("sdkVersion"),
                "Android/" + BuildConfig.VERSION_NAME);
    }

    @Test
    public void prepareLookup_initializesCardinal() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("fake-df")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, basicRequest, callback);

        verify(cardinalClient).initialize(same(activity), same(threeDSecureEnabledConfig),
                same(basicRequest), any(CardinalInitializeCallback.class));
    }

    @Test
    public void prepareLookup_whenCardinalClientInitializeFails_forwardsError()
            throws BraintreeException {
        BraintreeException initializeRuntimeError = new BraintreeException("initialize error");
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .initializeRuntimeError(initializeRuntimeError)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, basicRequest, callback);

        verify(callback).onResult(null, null, initializeRuntimeError);
    }

    @Test
    public void prepareLookup_withoutCardinalJWT_postsException() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(configuration)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, basicRequest, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult(isNull(), isNull(),
                captor.capture());

        TestCase.assertTrue(captor.getValue() instanceof BraintreeException);
        Assert.assertEquals(captor.getValue().getMessage(),
                "Merchant is not configured for 3DS 2.0. " +
                        "Please contact Braintree Support for assistance.");
    }

    // endregion

    // region createPaymentAuthRequest

    @Test
    public void createPaymentAuthRequest_sendsAnalyticEvent() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("sample-session-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        threeDSecureAPI);
        sut.createPaymentAuthRequest(activity, basicRequest, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.initialized");
    }

    @Test
    public void createPaymentAuthRequest_sendsParamsInLookupRequest()
            throws JSONException, BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("df-reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setNonce("a-nonce");
        request.setAmount("amount");
        request.setRequestedExemptionType(ThreeDSecureRequest.SECURE_CORPORATE);

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        request.setBillingAddress(billingAddress);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));
        sut.createPaymentAuthRequest(activity, request, threeDSecureResultCallback);

        String expectedUrl = "/v1/payment_methods/a-nonce/three_d_secure/lookup";
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq(expectedUrl), bodyCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject body = new JSONObject(bodyCaptor.getValue());
        assertEquals("amount", body.getString("amount"));
        assertEquals("df-reference-id", body.getString("df_reference_id"));
        assertEquals("billing-given-name",
                body.getJSONObject("additional_info").getString("billing_given_name"));
        assertEquals("secure_corporate", body.getString("requested_exemption_type"));
    }

    @Test
    public void createPaymentAuthRequest_performsLookup_WhenCardinalSDKInitFails()
            throws JSONException, BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .error(new Exception("error"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setNonce("a-nonce");
        request.setAmount("amount");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        request.setBillingAddress(billingAddress);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));
        sut.createPaymentAuthRequest(activity, request, threeDSecureResultCallback);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(pathCaptor.capture(), bodyCaptor.capture(),
                any(HttpResponseCallback.class));

        String path = pathCaptor.getValue();
        String body = bodyCaptor.getValue();
        JSONObject bodyJson = new JSONObject(body);

        assertEquals("/v1/payment_methods/a-nonce/three_d_secure/lookup", path);
        assertEquals("amount", bodyJson.get("amount"));
        assertFalse(bodyJson.getBoolean("challenge_requested"));
        assertFalse(bodyJson.getBoolean("data_only_requested"));
        assertFalse(bodyJson.getBoolean("exemption_requested"));
        JSONObject additionalInfo = bodyJson.getJSONObject("additional_info");
        assertEquals("billing-given-name", additionalInfo.get("billing_given_name"));
    }

    @Test
    public void createPaymentAuthRequest_callsLookupListener() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("sample-session-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
                .build();

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setNonce("a-nonce");
        request.setAmount("amount");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        request.setBillingAddress(billingAddress);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        sut.createPaymentAuthRequest(activity, request, threeDSecureResultCallback);

        verify(threeDSecureResultCallback).onResult(any(ThreeDSecureResult.class),
                isNull());
    }

    @Test
    public void createPaymentAuthRequest_withInvalidRequest_postsException() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        threeDSecureAPI);

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setAmount("5");
        sut.createPaymentAuthRequest(activity, request, threeDSecureResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureResultCallback).onResult(isNull(), captor.capture());
        assertEquals("The ThreeDSecureRequest nonce and amount cannot be null",
                captor.getValue().getMessage());
    }

    @Test
    public void createPaymentAuthRequest_initializesCardinal() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("df-reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));
        sut.createPaymentAuthRequest(activity, basicRequest, mock(ThreeDSecureResultCallback.class));

        verify(cardinalClient).initialize(same(activity), same(threeDSecureEnabledConfig),
                same(basicRequest), any(CardinalInitializeCallback.class));
    }

    @Test
    public void createPaymentAuthRequest_whenCardinalClientInitializeFails_forwardsError()
            throws BraintreeException {
        BraintreeException initializeRuntimeError = new BraintreeException("initialize error");
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .initializeRuntimeError(initializeRuntimeError)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.createPaymentAuthRequest(activity, basicRequest, callback);

        verify(callback).onResult(null, initializeRuntimeError);
    }

    @Test
    public void createPaymentAuthRequest_whenCardinalSetupCompleted_sendsAnalyticEvent()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("df-reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));
        sut.createPaymentAuthRequest(activity, basicRequest, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.cardinal-sdk.init.setup-completed");
    }

    @Test
    public void createPaymentAuthRequest_whenCardinalSetupFailed_sendsAnalyticEvent()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .error(new Exception("cardinal error"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));
        sut.createPaymentAuthRequest(activity, basicRequest, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-failed");
    }

    @Test
    public void createPaymentAuthRequest_withoutCardinalJWT_postsException() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(configuration)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.createPaymentAuthRequest(activity, basicRequest, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult(isNull(), captor.capture());
        TestCase.assertTrue(captor.getValue() instanceof BraintreeException);
        Assert.assertEquals(captor.getValue().getMessage(),
                "Merchant is not configured for 3DS 2.0. " +
                        "Please contact Braintree Support for assistance.");
    }

    // endregion

    // region sendAnalyticsAndCallbackResult
    @Test
    public void sendAnalyticsAndCallbackResult_whenAuthenticatingWithCardinal_sendsAnalyticsEvent()
            throws JSONException, BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
        sut.sendAnalyticsAndCallbackResult(threeDSecureResult, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.started");
    }

    @Test
    public void sendAnalyticsAndCallbackResult_whenChallengeIsPresented_sendsAnalyticsEvent()
            throws JSONException, BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .returnUrlScheme("sample-return-url://")
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);
        sut.sendAnalyticsAndCallbackResult(threeDSecureResult, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.challenge-presented.true");
    }

    @Test
    public void sendAnalyticsAndCallbackResult_whenChallengeIsNotPresented_sendsAnalyticsEvent()
            throws JSONException, BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL);
        sut.sendAnalyticsAndCallbackResult(threeDSecureResult, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.challenge-presented.false");
    }

    @Test
    public void sendAnalyticsAndCallbackResult_whenChallengeIsNotPresented_returnsResult()
            throws JSONException, BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL);
        sut.sendAnalyticsAndCallbackResult(threeDSecureResult, threeDSecureResultCallback);

        verify(threeDSecureResultCallback).onResult(threeDSecureResult, null);
    }

    @Test
    public void sendAnalyticsAndCallbackResult_sendsAnalyticsEvent()
            throws JSONException, BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
        sut.sendAnalyticsAndCallbackResult(threeDSecureResult, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.3ds-version.2.1.0");
    }

    @Test
    public void sendAnalyticsAndCallbackResult_callsBackThreeDSecureResultForLaunch()
            throws JSONException, BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));
        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        sut.sendAnalyticsAndCallbackResult(threeDSecureResult,
                threeDSecureResultCallback);

        verify(threeDSecureResultCallback).onResult(threeDSecureResult, null);
    }

    // endregion

    // region tokenize
    @Test
    public void tokenize_whenErrorExists_forwardsErrorToCallback_andSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        threeDSecureAPI);

        Exception threeDSecureError = new Exception("3DS error.");
        ThreeDSecurePaymentAuthResult paymentAuthResult = new ThreeDSecurePaymentAuthResult(threeDSecureError);
        sut.tokenize(paymentAuthResult, threeDSecureResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureResultCallback).onResult(isNull(), captor.capture());
    }

    @Test
    public void tokenize_onSuccess_sendsAnalyticsEvent() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        threeDSecureAPI);

        ThreeDSecurePaymentAuthResult paymentAuthResult =
                new ThreeDSecurePaymentAuthResult(threeDSecureResult, "jwt", validateResponse);
        sut.tokenize(paymentAuthResult, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.cardinal-sdk.action-code.success");
    }

    @Test
    public void tokenize_whenValidateResponseTimeout_returnsErrorAndSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.TIMEOUT);
        when(validateResponse.getErrorDescription()).thenReturn("Error");

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        threeDSecureAPI);

        ThreeDSecurePaymentAuthResult paymentAuthResult =
                new ThreeDSecurePaymentAuthResult(threeDSecureResult, "jwt", validateResponse);
        sut.tokenize(paymentAuthResult, threeDSecureResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureResultCallback).onResult(isNull(), captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("Error", captor.getValue().getMessage());

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.failed");
    }

    @Test
    public void tokenize_whenValidateResponseCancel_returnsUserCanceledErrorAndSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.CANCEL);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        threeDSecureAPI);

        ThreeDSecurePaymentAuthResult paymentAuthResult =
                new ThreeDSecurePaymentAuthResult(threeDSecureResult, "jwt", validateResponse);
        sut.tokenize(paymentAuthResult, threeDSecureResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureResultCallback).onResult(isNull(), captor.capture());
        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled 3DS.", exception.getMessage());
        assertTrue(((UserCanceledException) exception).isExplicitCancelation());

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.canceled");
    }

    @Test
    public void tokenize_whenValidateResponseSuccess_onAuthenticateCardinalJWTResult_returnsResultAndSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        doAnswer((Answer<Void>) invocation -> {
            ThreeDSecureResultCallback callback =
                    (ThreeDSecureResultCallback) invocation.getArguments()[2];
            callback.onResult(threeDSecureResult, null);
            return null;
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureResult.class), anyString(),
                any(ThreeDSecureResultCallback.class));

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        threeDSecureAPI);

        ThreeDSecurePaymentAuthResult paymentAuthResult =
                new ThreeDSecurePaymentAuthResult(threeDSecureResult, "jwt", validateResponse);
        sut.tokenize(paymentAuthResult, threeDSecureResultCallback);

        verify(threeDSecureResultCallback).onResult(threeDSecureResult, null);
        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.upgrade-payment-method.succeeded");
        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.liability-shifted.true");
        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.liability-shift-possible.true");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
    }

    @Test
    public void tokenize_whenValidateResponseSuccess_onAuthenticateCardinalJWTResultWithError_returnsResultAndSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        final ThreeDSecureResult threeDSecureResult = mock(ThreeDSecureResult.class);
        when(threeDSecureResult.hasError()).thenReturn(true);

        doAnswer((Answer<Void>) invocation -> {
            ThreeDSecureResultCallback callback =
                    (ThreeDSecureResultCallback) invocation.getArguments()[2];
            callback.onResult(threeDSecureResult, null);
            return null;
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureResult.class), anyString(),
                any(ThreeDSecureResultCallback.class));

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        threeDSecureAPI);

        ThreeDSecurePaymentAuthResult paymentAuthResult =
                new ThreeDSecurePaymentAuthResult(threeDSecureResult, "jwt", validateResponse);
        sut.tokenize(paymentAuthResult, threeDSecureResultCallback);

        verify(threeDSecureResultCallback).onResult(threeDSecureResult, null);
        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
    }

    @Test
    public void tokenize_whenValidateResponseSuccess_onAuthenticateCardinalJWTError_returnsErrorAndSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        final Exception exception = new Exception("error");

        doAnswer((Answer<Void>) invocation -> {
            ThreeDSecureResultCallback callback =
                    (ThreeDSecureResultCallback) invocation.getArguments()[2];
            callback.onResult(null, exception);
            return null;
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureResult.class), anyString(),
                any(ThreeDSecureResultCallback.class));

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        threeDSecureAPI);

        ThreeDSecurePaymentAuthResult paymentAuthResult =
                new ThreeDSecurePaymentAuthResult(threeDSecureResult, "jwt", validateResponse);
        sut.tokenize(paymentAuthResult, threeDSecureResultCallback);

        verify(threeDSecureResultCallback).onResult(null, exception);
        braintreeClient.sendAnalyticsEvent(
                "three-d-secure.verification-flow.upgrade-payment-method.errored");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
    }

    // endregion
}
