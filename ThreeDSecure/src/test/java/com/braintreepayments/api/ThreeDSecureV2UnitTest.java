package com.braintreepayments.api;

import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.BraintreeRequestCodes.THREE_D_SECURE;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2UnitTest {

    private FragmentActivity activity;
    private ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper;
    private ThreeDSecureListener listener;
    private Lifecycle lifecycle;

    private Configuration threeDSecureEnabledConfig;
    private ThreeDSecureRequest basicRequest;

    @Before
    public void setup() {
        activity = mock(FragmentActivity.class);
        browserSwitchHelper = mock(ThreeDSecureV1BrowserSwitchHelper.class);
        listener = mock(ThreeDSecureListener.class);

        threeDSecureEnabledConfig = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .cardinalAuthenticationJWT("cardinal_authentication_jwt")
                .buildConfiguration();

        ThreeDSecureV2TextBoxCustomization textBoxCustomization = new ThreeDSecureV2TextBoxCustomization();
        textBoxCustomization.setBorderWidth(12);

        ThreeDSecureV2UiCustomization v2UiCustomization = new ThreeDSecureV2UiCustomization();
        v2UiCustomization.setTextBoxCustomization(textBoxCustomization);

        basicRequest = new ThreeDSecureRequest();
        basicRequest.setNonce("a-nonce");
        basicRequest.setAmount("1.00");
        basicRequest.setVersionRequested(ThreeDSecureRequest.VERSION_2);
        basicRequest.setV2UiCustomization(v2UiCustomization);

        ActivityResultRegistry resultRegistry = mock(ActivityResultRegistry.class);
        when(activity.getActivityResultRegistry()).thenReturn(resultRegistry);

        lifecycle = mock(Lifecycle.class);
        when(activity.getLifecycle()).thenReturn(lifecycle);
    }

    @Test
    public void prepareLookup_returnsValidLookupJSONString() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("fake-df")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, basicRequest, callback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(callback).onResult(same(basicRequest), captor.capture(), (Exception) isNull());

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
    public void prepareLookup_returnsValidLookupJSONString_whenCardinalSetupFails() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .error(new Exception("cardinal error"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, basicRequest, callback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(callback).onResult(same(basicRequest), captor.capture(), (Exception) isNull());

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
    public void prepareLookup_initializesCardinal() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("fake-df")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, basicRequest, callback);

        verify(cardinalClient).initialize(same(activity), same(threeDSecureEnabledConfig), same(basicRequest), any(CardinalInitializeCallback.class));
    }

    @Test
    public void prepareLookup_withoutCardinalJWT_postsException() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(configuration)
                .build();
        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));

        ThreeDSecurePrepareLookupCallback callback = mock(ThreeDSecurePrepareLookupCallback.class);
        sut.prepareLookup(activity, basicRequest, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((ThreeDSecureRequest) isNull(), (String) isNull(), captor.capture());

        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals(captor.getValue().getMessage(), "Merchant is not configured for 3DS 2.0. " +
                "Please contact Braintree Support for assistance.");
    }

    @Test
    public void performVerification_initializesCardinal() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("df-reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);
        sut.performVerification(activity, basicRequest, mock(ThreeDSecureResultCallback.class));

        verify(cardinalClient).initialize(same(activity), same(threeDSecureEnabledConfig), same(basicRequest), any(CardinalInitializeCallback.class));
    }

    @Test
    public void performVerification_whenCardinalSetupCompleted_sendsAnalyticEvent() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("df-reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);
        sut.performVerification(activity, basicRequest, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-completed");
    }

    @Test
    public void performVerification_whenCardinalSetupFailed_sendsAnalyticEvent() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .error(new Exception("cardinal error"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);
        sut.performVerification(activity, basicRequest, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-failed");
    }

    @Test
    public void continuePerformVerification_whenAuthenticatingWithCardinal_sendsAnalyticsEvent() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);
        sut.addObserver(activity, lifecycle);
        sut.observer.activityLauncher = mock(ActivityResultLauncher.class);

        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
        sut.continuePerformVerification(activity, basicRequest, threeDSecureResult);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.started");
    }

    @Test
    public void continuePerformVerification_whenChallengeIsPresented_sendsAnalyticsEvent() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .returnUrlScheme("sample-return-url://")
                .build();
        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);
        when(browserSwitchHelper.getUrl(anyString(), anyString(), any(ThreeDSecureRequest.class), any(ThreeDSecureLookup.class))).thenReturn("https://example.com");

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);

        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);
        sut.continuePerformVerification(activity, basicRequest, threeDSecureResult);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.challenge-presented.true");
    }

    @Test
    public void continuePerformVerification_whenChallengeIsNotPresented_sendsAnalyticsEvent() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);

        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL);
        sut.continuePerformVerification(activity, basicRequest, threeDSecureResult);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.challenge-presented.false");
    }

    @Test
    public void continuePerformVerification_whenChallengeIsNotPresented_returnsResult() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);

        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL);
        sut.continuePerformVerification(activity, basicRequest, threeDSecureResult);

        verify(listener).onThreeDSecureSuccess(threeDSecureResult);
    }

    @Test
    public void continuePerformVerification_when3DSVersionIsVersion2_sendsAnalyticsEvent() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);
        sut.addObserver(activity, lifecycle);
        sut.observer.activityLauncher = mock(ActivityResultLauncher.class);

        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
        sut.continuePerformVerification(activity, basicRequest, threeDSecureResult);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.3ds-version.2.1.0");
    }

    @Test
    public void continuePerformVerification_whenObserverIsNull_startsActivity() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(null, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        sut.continuePerformVerification(activity, basicRequest, threeDSecureResult, mock(ThreeDSecureResultCallback.class));

        verify(activity).startActivityForResult(any(Intent.class), any(Integer.class));
    }


    @Test
    public void performVerification_withoutCardinalJWT_postsException() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .buildConfiguration();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(configuration)
                .build();
        when(braintreeClient.assertCanPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.performVerification(activity, basicRequest, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((ThreeDSecureResult) isNull(), captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals(captor.getValue().getMessage(), "Merchant is not configured for 3DS 2.0. " +
                "Please contact Braintree Support for assistance.");
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationReportsSuccess_sendsAnalyticsEvent() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        ThreeDSecureAPI threeDSecureAPI = mock(ThreeDSecureAPI.class);

        final ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ThreeDSecureResultCallback callback = (ThreeDSecureResultCallback) invocation.getArguments()[2];
                callback.onResult(threeDSecureResult, null);
                return null;
            }
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureResult.class), (String) isNull(), any(ThreeDSecureResultCallback.class));

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);
        data.putExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, threeDSecureResult);

        sut.onActivityResult(RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.cardinal-sdk.action-code.success");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.liability-shifted.true");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.liability-shift-possible.true");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationReportsSuccess_whenResultWithError_sendsAnalytics() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        ThreeDSecureAPI threeDSecureAPI = mock(ThreeDSecureAPI.class);

        final ThreeDSecureResult threeDSecureResult = mock(ThreeDSecureResult.class);
        when(threeDSecureResult.hasError()).thenReturn(true);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ThreeDSecureResultCallback callback = (ThreeDSecureResultCallback) invocation.getArguments()[2];
                callback.onResult(threeDSecureResult, null);
                return null;
            }
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureResult.class), (String) isNull(), any(ThreeDSecureResultCallback.class));

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);
        data.putExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, threeDSecureResult);

        sut.onActivityResult(RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationReportsSuccess_whenAuthenticateJWTReturnsError_sendsAnalytics() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        ThreeDSecureAPI threeDSecureAPI = mock(ThreeDSecureAPI.class);

        final ThreeDSecureResult threeDSecureResult = mock(ThreeDSecureResult.class);
        final Exception exception = new Exception("error");

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ThreeDSecureResultCallback callback = (ThreeDSecureResultCallback) invocation.getArguments()[2];
                callback.onResult(null, exception);
                return null;
            }
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureResult.class), (String) isNull(), any(ThreeDSecureResultCallback.class));

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);
        data.putExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, threeDSecureResult);

        sut.onActivityResult(RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.errored");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationReportsNoAction_sendsAnalyticsEvent() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));

        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.NOACTION);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);
        data.putExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, threeDSecureResult);

        sut.onActivityResult(RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.cardinal-sdk.action-code.noaction");
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationReportsFailure_sendsAnalyticsEvent() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));

        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.FAILURE);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);
        data.putExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, threeDSecureResult);

        sut.onActivityResult(RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.cardinal-sdk.action-code.failure");
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationIsCanceled_sendsAnalyticsEventAndReturnsExceptionToCallback() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.CANCEL);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);

        ThreeDSecureResultCallback threeDSecureResultCallback = mock(ThreeDSecureResultCallback.class);
        sut.onActivityResult(RESULT_OK, data, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.canceled");

        ArgumentCaptor<BraintreeException> captor = ArgumentCaptor.forClass(BraintreeException.class);
        verify(threeDSecureResultCallback).onResult((ThreeDSecureResult) isNull(), captor.capture());

        BraintreeException exception = captor.getValue();
        assertEquals("User canceled 3DS.", exception.getMessage());
        assertTrue(exception instanceof UserCanceledException);
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationErrors_sendsAnalyticsEvent() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.ERROR);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);

        sut.onActivityResult(RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.failed");
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationTimeout_sendsAnalyticsEvent() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.TIMEOUT);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);

        sut.onActivityResult(RESULT_OK, data, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.failed");
    }
}
