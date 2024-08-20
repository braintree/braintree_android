package com.braintreepayments.api.threedsecure;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.core.Authorization;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.testutils.Fixtures;
import com.braintreepayments.api.sharedutils.HttpResponseCallback;
import com.braintreepayments.api.testutils.MockBraintreeClientBuilder;
import com.braintreepayments.api.testutils.TestConfigurationBuilder;
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

    private ThreeDSecurePaymentAuthRequestCallback paymentAuthRequestCallback;
    private ThreeDSecureTokenizeCallback threeDSecureTokenizeCallback;

    private Configuration threeDSecureEnabledConfig;

    ThreeDSecureRequest basicRequest;
    ThreeDSecureParams threeDSecureParams;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        paymentAuthRequestCallback = mock(ThreeDSecurePaymentAuthRequestCallback.class);
        threeDSecureTokenizeCallback = mock(ThreeDSecureTokenizeCallback.class);
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

        threeDSecureParams =
                ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
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

        ArgumentCaptor<ThreeDSecurePrepareLookupResult> captor = ArgumentCaptor.forClass(ThreeDSecurePrepareLookupResult.class);
        verify(callback).onPrepareLookupResult(captor.capture());

        ThreeDSecurePrepareLookupResult prepareLookupResult = captor.getValue();
        assertTrue(prepareLookupResult instanceof ThreeDSecurePrepareLookupResult.Success);
        assertSame(basicRequest, ((ThreeDSecurePrepareLookupResult.Success) prepareLookupResult).getRequest());


        String clientData = ((ThreeDSecurePrepareLookupResult.Success) prepareLookupResult).getClientData();
        JSONObject lookup = new JSONObject(clientData);
        Assert.assertEquals("encoded_auth_fingerprint",
                lookup.getString("authorizationFingerprint"));
        Assert.assertEquals(lookup.getString("braintreeLibraryVersion"),
                "Android-" + com.braintreepayments.api.core.BuildConfig.VERSION_NAME);
        Assert.assertEquals(lookup.getString("dfReferenceId"), "fake-df");
        Assert.assertEquals(lookup.getString("nonce"), "a-nonce");

        JSONObject clientMetaData = lookup.getJSONObject("clientMetadata");
        Assert.assertEquals(clientMetaData.getString("requestedThreeDSecureVersion"), "2");
        Assert.assertEquals(clientMetaData.getString("sdkVersion"),
                "Android/" + com.braintreepayments.api.core.BuildConfig.VERSION_NAME);
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

        ArgumentCaptor<ThreeDSecurePrepareLookupResult> captor = ArgumentCaptor.forClass(ThreeDSecurePrepareLookupResult.class);
        verify(callback).onPrepareLookupResult(captor.capture());

        ThreeDSecurePrepareLookupResult prepareLookupResult = captor.getValue();
        assertTrue(prepareLookupResult instanceof ThreeDSecurePrepareLookupResult.Success);
        assertSame(basicRequest, ((ThreeDSecurePrepareLookupResult.Success) prepareLookupResult).getRequest());

        String clientData = ((ThreeDSecurePrepareLookupResult.Success) prepareLookupResult).getClientData();
        JSONObject lookup = new JSONObject(clientData);
        Assert.assertEquals("encoded_auth_fingerprint",
                lookup.getString("authorizationFingerprint"));
        Assert.assertEquals(lookup.getString("braintreeLibraryVersion"),
                "Android-" + com.braintreepayments.api.core.BuildConfig.VERSION_NAME);
        Assert.assertEquals(lookup.getString("nonce"), "a-nonce");
        assertFalse(lookup.has("dfReferenceId"));

        JSONObject clientMetaData = lookup.getJSONObject("clientMetadata");
        Assert.assertEquals(clientMetaData.getString("requestedThreeDSecureVersion"), "2");
        Assert.assertEquals(clientMetaData.getString("sdkVersion"),
                "Android/" + com.braintreepayments.api.core.BuildConfig.VERSION_NAME);
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

        ArgumentCaptor<ThreeDSecurePrepareLookupResult> captor = ArgumentCaptor.forClass(ThreeDSecurePrepareLookupResult.class);
        verify(callback).onPrepareLookupResult(captor.capture());
        ThreeDSecurePrepareLookupResult prepareLookupResult = captor.getValue();
        assertTrue(prepareLookupResult instanceof ThreeDSecurePrepareLookupResult.Failure);
        assertEquals(initializeRuntimeError, ((ThreeDSecurePrepareLookupResult.Failure) prepareLookupResult).getError());
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

        ArgumentCaptor<ThreeDSecurePrepareLookupResult> captor = ArgumentCaptor.forClass(ThreeDSecurePrepareLookupResult.class);
        verify(callback).onPrepareLookupResult(captor.capture());
        ThreeDSecurePrepareLookupResult prepareLookupResult = captor.getValue();
        assertTrue(prepareLookupResult instanceof ThreeDSecurePrepareLookupResult.Failure);
        Exception error = ((ThreeDSecurePrepareLookupResult.Failure) prepareLookupResult).getError();

        TestCase.assertTrue(error instanceof BraintreeException);
        Assert.assertEquals(error.getMessage(),
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
        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback);

        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_STARTED);
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
        sut.createPaymentAuthRequest(activity, request, paymentAuthRequestCallback);

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
        sut.createPaymentAuthRequest(activity, request, paymentAuthRequestCallback);

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

        sut.createPaymentAuthRequest(activity, request, paymentAuthRequestCallback);

        verify(paymentAuthRequestCallback).onThreeDSecurePaymentAuthRequest(any(ThreeDSecurePaymentAuthRequest.class));
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
        sut.createPaymentAuthRequest(activity, request, paymentAuthRequestCallback);

        ArgumentCaptor<ThreeDSecurePaymentAuthRequest> captor = ArgumentCaptor.forClass(ThreeDSecurePaymentAuthRequest.class);
        verify(paymentAuthRequestCallback).onThreeDSecurePaymentAuthRequest(captor.capture());
        ThreeDSecurePaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof ThreeDSecurePaymentAuthRequest.Failure);
        assertEquals("The ThreeDSecureRequest nonce and amount cannot be null",
                ((ThreeDSecurePaymentAuthRequest.Failure) paymentAuthRequest).getError().getMessage());
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
        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback);

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

        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback);

        ArgumentCaptor<ThreeDSecurePaymentAuthRequest> captor = ArgumentCaptor.forClass(ThreeDSecurePaymentAuthRequest.class);
        verify(paymentAuthRequestCallback).onThreeDSecurePaymentAuthRequest(captor.capture());
        ThreeDSecurePaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof ThreeDSecurePaymentAuthRequest.Failure);
        assertEquals(initializeRuntimeError, ((ThreeDSecurePaymentAuthRequest.Failure) paymentAuthRequest).getError());
    }


    @Test
    public void createPaymentAuthRequest_whenCardinalSetupFailed_sendsAnalyticEvent()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .initializeRuntimeError(new BraintreeException("cardinal error"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));
        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback);

        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_STARTED);
        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_FAILED);
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

        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback);

        ArgumentCaptor<ThreeDSecurePaymentAuthRequest> captor = ArgumentCaptor.forClass(ThreeDSecurePaymentAuthRequest.class);
        verify(paymentAuthRequestCallback).onThreeDSecurePaymentAuthRequest(captor.capture());
        ThreeDSecurePaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof ThreeDSecurePaymentAuthRequest.Failure);
        Exception error = ((ThreeDSecurePaymentAuthRequest.Failure) paymentAuthRequest).getError();
        TestCase.assertTrue(error instanceof BraintreeException);
        Assert.assertEquals(error.getMessage(),
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

        ThreeDSecureParams threeDSecureParams =
                ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
        sut.sendAnalyticsAndCallbackResult(threeDSecureParams, paymentAuthRequestCallback);

        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED);
    }

    @Test
    public void sendAnalyticsAndCallbackResult_whenChallengeIsRequired_sendsAnalyticsEvent()
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

        ThreeDSecureParams threeDSecureParams =
                ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);
        sut.sendAnalyticsAndCallbackResult(threeDSecureParams, paymentAuthRequestCallback);

        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED);
        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.CHALLENGE_REQUIRED);
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

        ThreeDSecureParams threeDSecureParams =
                ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL);
        sut.sendAnalyticsAndCallbackResult(threeDSecureParams, paymentAuthRequestCallback);

        ArgumentCaptor<ThreeDSecurePaymentAuthRequest> captor = ArgumentCaptor.forClass(ThreeDSecurePaymentAuthRequest.class);
        verify(paymentAuthRequestCallback).onThreeDSecurePaymentAuthRequest(captor.capture());
        ThreeDSecurePaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof ThreeDSecurePaymentAuthRequest.LaunchNotRequired);
        assertEquals(threeDSecureParams.getThreeDSecureNonce(), ((ThreeDSecurePaymentAuthRequest.LaunchNotRequired) paymentAuthRequest).getNonce());
        assertEquals(threeDSecureParams.getLookup(), ((ThreeDSecurePaymentAuthRequest.LaunchNotRequired) paymentAuthRequest).getThreeDSecureLookup());
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
        ThreeDSecureParams threeDSecureParams =
                ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        sut.sendAnalyticsAndCallbackResult(threeDSecureParams,
                paymentAuthRequestCallback);

        ArgumentCaptor<ThreeDSecurePaymentAuthRequest> captor = ArgumentCaptor.forClass(ThreeDSecurePaymentAuthRequest.class);
        verify(paymentAuthRequestCallback).onThreeDSecurePaymentAuthRequest(captor.capture());
        ThreeDSecurePaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof ThreeDSecurePaymentAuthRequest.ReadyToLaunch);
        assertEquals(threeDSecureParams, ((ThreeDSecurePaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams());
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
        ThreeDSecurePaymentAuthResult paymentAuthResult = new ThreeDSecurePaymentAuthResult(null, null, null, threeDSecureError);
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback);

        ArgumentCaptor<ThreeDSecureResult> captor = ArgumentCaptor.forClass(ThreeDSecureResult.class);
        verify(threeDSecureTokenizeCallback).onThreeDSecureResult(captor.capture());
        ThreeDSecureResult result = captor.getValue();
        assertTrue(result instanceof ThreeDSecureResult.Failure);
        assertEquals(threeDSecureError, ((ThreeDSecureResult.Failure) result).getError());
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
            new ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null);
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback);

        ArgumentCaptor<ThreeDSecureResult> captor = ArgumentCaptor.forClass(ThreeDSecureResult.class);
        verify(threeDSecureTokenizeCallback).onThreeDSecureResult(captor.capture());
        ThreeDSecureResult result = captor.getValue();
        assertTrue(result instanceof ThreeDSecureResult.Failure);

        Exception error = ((ThreeDSecureResult.Failure) result).getError();
        assertTrue(error instanceof BraintreeException);
        assertEquals("Error", error.getMessage());

        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_FAILED);
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
            new ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null);
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback);

        ArgumentCaptor<ThreeDSecureResult> captor = ArgumentCaptor.forClass(ThreeDSecureResult.class);
        verify(threeDSecureTokenizeCallback).onThreeDSecureResult(captor.capture());
        ThreeDSecureResult result = captor.getValue();
        assertTrue(result instanceof ThreeDSecureResult.Cancel);

        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_CANCELED);
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
            callback.onThreeDSecureResult(threeDSecureParams, null);
            return null;
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureParams.class), anyString(),
                any(ThreeDSecureResultCallback.class));

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        threeDSecureAPI);

        ThreeDSecurePaymentAuthResult paymentAuthResult =
            new ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null);
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback);

        ArgumentCaptor<ThreeDSecureResult> captor = ArgumentCaptor.forClass(ThreeDSecureResult.class);
        verify(threeDSecureTokenizeCallback).onThreeDSecureResult(captor.capture());
        ThreeDSecureResult result = captor.getValue();
        assertTrue(result instanceof ThreeDSecureResult.Success);
        assertEquals(((ThreeDSecureResult.Success) result).getNonce(), threeDSecureParams.getThreeDSecureNonce());
        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.JWT_AUTH_SUCCEEDED);
        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_SUCCEEDED);
    }

    @Test
    public void tokenize_whenValidateResponseSuccess_onAuthenticateCardinalJWTResultWithError_returnsResultAndSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        final ThreeDSecureParams threeDSecureParams = mock(ThreeDSecureParams.class);
        when(threeDSecureParams.hasError()).thenReturn(true);

        doAnswer((Answer<Void>) invocation -> {
            ThreeDSecureResultCallback callback =
                    (ThreeDSecureResultCallback) invocation.getArguments()[2];
            callback.onThreeDSecureResult(threeDSecureParams, null);
            return null;
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureParams.class), anyString(),
                any(ThreeDSecureResultCallback.class));

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        threeDSecureAPI);

        ThreeDSecurePaymentAuthResult paymentAuthResult =
            new ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null);
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback);

        ArgumentCaptor<ThreeDSecureResult> captor = ArgumentCaptor.forClass(ThreeDSecureResult.class);
        verify(threeDSecureTokenizeCallback).onThreeDSecureResult(captor.capture());
        ThreeDSecureResult result = captor.getValue();
        assertTrue(result instanceof ThreeDSecureResult.Failure);
        assertEquals(((ThreeDSecureResult.Failure) result).getNonce(), paymentAuthResult.getThreeDSecureParams().getThreeDSecureNonce());

        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.JWT_AUTH_FAILED);
        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_FAILED);
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
            callback.onThreeDSecureResult(null, exception);
            return null;
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureParams.class), anyString(),
                any(ThreeDSecureResultCallback.class));

        ThreeDSecureClient sut =
                new ThreeDSecureClient(braintreeClient, cardinalClient,
                        threeDSecureAPI);

        ThreeDSecurePaymentAuthResult paymentAuthResult =
            new ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null);
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback);

        ArgumentCaptor<ThreeDSecureResult> captor = ArgumentCaptor.forClass(ThreeDSecureResult.class);
        verify(threeDSecureTokenizeCallback).onThreeDSecureResult(captor.capture());
        ThreeDSecureResult result = captor.getValue();
        assertTrue(result instanceof ThreeDSecureResult.Failure);
        assertEquals(exception, ((ThreeDSecureResult.Failure) result).getError());

        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.JWT_AUTH_FAILED);
        verify(braintreeClient).sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_FAILED);
    }

    // endregion
}
