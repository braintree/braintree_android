package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BraintreeClientUnitTest {

    private Authorization authorization;
    private Context context;
    private Context applicationContext;

    private BraintreeHttpClient braintreeHttpClient;
    private BraintreeGraphQLClient braintreeGraphQLClient;
    private ConfigurationLoader configurationLoader;
    private AuthorizationLoader authorizationLoader;
    private AnalyticsClient analyticsClient;
    private ManifestValidator manifestValidator;
    private BrowserSwitchClient browserSwitchClient;

    @Before
    public void beforeEach() {
        authorization = mock(Authorization.class);
        context = mock(Context.class);
        applicationContext = ApplicationProvider.getApplicationContext();

        braintreeHttpClient = mock(BraintreeHttpClient.class);
        braintreeGraphQLClient = mock(BraintreeGraphQLClient.class);
        configurationLoader = mock(ConfigurationLoader.class);
        authorizationLoader = mock(AuthorizationLoader.class);
        analyticsClient = mock(AnalyticsClient.class);
        manifestValidator = mock(ManifestValidator.class);
        browserSwitchClient = mock(BrowserSwitchClient.class);

        when(context.getApplicationContext()).thenReturn(applicationContext);

        WorkManagerTestInitHelper.initializeTestWorkManager(context);
    }

    @Test
    public void constructor_usesSessionIdFromParams() {
        BraintreeClientParams params = new BraintreeClientParams()
                .context(context)
                .sessionId("session-id");
        BraintreeClient sut = new BraintreeClient(params);

        assertEquals("session-id", sut.getSessionId());
    }

    @Test
    public void constructor_setsSessionIdFromUUIDHelperIfSessionIdNotIncluded() {
        UUIDHelper uuidHelper = mock(UUIDHelper.class);
        when(uuidHelper.getFormattedUUID()).thenReturn("sample-formatted-uuid");

        BraintreeClientParams params = new BraintreeClientParams()
                .context(context)
                .UUIDHelper(uuidHelper);
        BraintreeClient sut = new BraintreeClient(params);

        assertEquals("sample-formatted-uuid", sut.getSessionId());
    }

    @Test
    public void getConfiguration_onAuthorizationLoaderSuccess_forwardsInvocationToConfigurationLoader() {
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorization(authorization)
                .build();
        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        ConfigurationCallback callback = mock(ConfigurationCallback.class);
        sut.getConfiguration(callback);

        verify(configurationLoader).loadConfiguration(applicationContext, authorization, callback);
    }

    @Test
    public void getConfiguration_forwardsAuthorizationLoaderError() {
        Exception authFetchError = new Exception("auth fetch error");
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorizationError(authFetchError)
                .build();
        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        ConfigurationCallback callback = mock(ConfigurationCallback.class);
        sut.getConfiguration(callback);

        verify(callback).onResult(null, authFetchError);
    }

    @Test
    public void getAuthorization_forwardsInvocationToAuthorizationLoader() {
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder().build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        AuthorizationCallback callback = mock(AuthorizationCallback.class);
        sut.getAuthorization(callback);

        verify(authorizationLoader).loadAuthorization(callback);
    }

    @Test
    public void invalidateClientToken_forwardsInvocationToAuthorizationLoader() {
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder().build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        sut.invalidateClientToken();

        verify(authorizationLoader).invalidateClientToken();
    }

    @Test
    public void sendGET_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        Configuration configuration = mock(Configuration.class);
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorization(authorization)
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configuration(configuration)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGET("sample-url", httpResponseCallback);

        verify(braintreeHttpClient).get(eq("sample-url"), same(configuration), same(authorization), same(httpResponseCallback));
    }

    @Test
    public void sendGET_onGetAuthorizationFailure_forwardsErrorToCallback() {
        Exception authorizationError = new Exception("authorization error");
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorizationError(authorizationError)
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder().build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGET("sample-url", httpResponseCallback);

        verify(httpResponseCallback).onResult((String) isNull(), same(authorizationError));
    }

    @Test
    public void sendGET_onGetConfigurationFailure_forwardsErrorToCallback() {
        Exception configError = new Exception("configuration error");
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorization(authorization)
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configurationError(configError)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGET("sample-url", httpResponseCallback);

        verify(httpResponseCallback).onResult((String) isNull(), same(configError));
    }

    @Test
    public void sendPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        Configuration configuration = mock(Configuration.class);
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorization(authorization)
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configuration(configuration)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendPOST("sample-url", "{}", httpResponseCallback);

        verify(braintreeHttpClient).post(eq("sample-url"), eq("{}"), same(configuration), same(authorization), same(httpResponseCallback));
    }

    @Test
    public void sendPOST_onAuthorizationFailure_forwardsErrorToCallback() {
        Exception authError = new Exception("authorization error");
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorizationError(authError)
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder().build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendPOST("sample-url", "{}", httpResponseCallback);

        verify(httpResponseCallback).onResult(null, authError);
    }

    @Test
    public void sendPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        Exception exception = new Exception("configuration error");
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorization(authorization)
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configurationError(exception)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendPOST("sample-url", "{}", httpResponseCallback);

        verify(httpResponseCallback).onResult(null, exception);
    }

    @Test
    public void sendGraphQLPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        Configuration configuration = mock(Configuration.class);
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorization(authorization)
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configuration(configuration)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGraphQLPOST("{}", httpResponseCallback);

        verify(braintreeGraphQLClient).post(eq("{}"), same(configuration), same(authorization), same(httpResponseCallback));
    }

    @Test
    public void sendGraphQLPOST_onAuthorizationFailure_forwardsErrorToCallback() {
        Exception authError = new Exception("authorization error");
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorizationError(authError)
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder().build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGraphQLPOST("{}", httpResponseCallback);

        verify(httpResponseCallback).onResult(null, authError);
    }

    @Test
    public void sendGraphQLPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        Exception exception = new Exception("configuration error");
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorization(authorization)
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configurationError(exception)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGraphQLPOST("{}", httpResponseCallback);

        verify(httpResponseCallback).onResult(null, exception);
    }

    @Test
    public void sendAnalyticsEvent_sendsEventToAnalyticsClient() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorization(authorization)
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configuration(configuration)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);
        sut.sendAnalyticsEvent("event.started");

        verify(analyticsClient).sendEvent(configuration, "event.started", "session-id", "custom", authorization);
    }

    @Test
    public void sendAnalyticsEvent_whenAuthorizationLoadFails_doesNothing() {
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorizationError(new Exception("error"))
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder().build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);
        sut.sendAnalyticsEvent("event.started");

        verifyZeroInteractions(analyticsClient);
    }

    @Test
    public void sendAnalyticsEvent_whenConfigurationLoadFails_doesNothing() {
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorization(authorization)
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configurationError(new Exception("error"))
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);
        sut.sendAnalyticsEvent("event.started");

        verifyZeroInteractions(analyticsClient);
    }

    @Test
    public void sendAnalyticsEvent_whenAnalyticsNotEnabled_doesNothing() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ANALYTICS);
        AuthorizationLoader authorizationLoader = new MockAuthorizationLoaderBuilder()
                .authorization(authorization)
                .build();
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configuration(configuration)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);
        sut.sendAnalyticsEvent("event.started");

        verifyZeroInteractions(analyticsClient);
    }

    @Test
    public void startBrowserSwitch_forwardsInvocationToBrowserSwitchClient() throws BrowserSwitchException {
        FragmentActivity activity = mock(FragmentActivity.class);
        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        sut.startBrowserSwitch(activity, browserSwitchOptions);
        verify(browserSwitchClient).start(activity, browserSwitchOptions);
    }

    @Test
    public void getBrowserSwitchResult_forwardsInvocationToBrowserSwitchClient() {
        FragmentActivity activity = mock(FragmentActivity.class);
        BrowserSwitchResult browserSwitchResult = createSuccessfulBrowserSwitchResult();
        when(browserSwitchClient.getResult(activity)).thenReturn(browserSwitchResult);

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        assertSame(browserSwitchResult, sut.getBrowserSwitchResult(activity));
    }

    @Test
    public void deliverBrowserSwitchResult_forwardsInvocationToBrowserSwitchClient() {
        FragmentActivity activity = mock(FragmentActivity.class);

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        sut.deliverBrowserSwitchResult(activity);
        verify(browserSwitchClient).deliverResult(activity);
    }

    @Test
    public void canPerformBrowserSwitch_assertsBrowserSwitchIsPossible() throws BrowserSwitchException {
        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        FragmentActivity activity = mock(FragmentActivity.class);
        sut.canPerformBrowserSwitch(activity, 123);

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(browserSwitchClient).assertCanPerformBrowserSwitch(same(activity), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(123, browserSwitchOptions.getRequestCode());
        assertEquals(Uri.parse("https://braintreepayments.com"), browserSwitchOptions.getUrl());
    }

    @Test
    public void canPerformBrowserSwitch_onSuccess_returnsTrue() throws BrowserSwitchException {
        FragmentActivity activity = mock(FragmentActivity.class);
        doNothing().when(browserSwitchClient).assertCanPerformBrowserSwitch(same(activity), any(BrowserSwitchOptions.class));

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        assertTrue(sut.canPerformBrowserSwitch(activity, 123));
    }

    @Test
    public void canPerformBrowserSwitch_onError_returnsFalse() throws BrowserSwitchException {
        FragmentActivity activity = mock(FragmentActivity.class);
        doThrow(new BrowserSwitchException("error")).when(browserSwitchClient).assertCanPerformBrowserSwitch(same(activity), any(BrowserSwitchOptions.class));

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        sut.canPerformBrowserSwitch(activity, 123);
        assertFalse(sut.canPerformBrowserSwitch(activity, 123));
    }

    @Test
    public void isUrlSchemeDeclaredInAndroidManifest_forwardsInvocationToManifestValidator() {
        when(manifestValidator.isUrlSchemeDeclaredInAndroidManifest(applicationContext, "a-url-scheme", FragmentActivity.class)).thenReturn(true);

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        assertTrue(sut.isUrlSchemeDeclaredInAndroidManifest("a-url-scheme", FragmentActivity.class));
    }

    @Test
    public void getManifestActivityInfo_forwardsInvocationToManifestValidator() {
        ActivityInfo activityInfo = new ActivityInfo();
        when(manifestValidator.getActivityInfo(applicationContext, FragmentActivity.class)).thenReturn(activityInfo);

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        assertSame(activityInfo, sut.getManifestActivityInfo(FragmentActivity.class));
    }

    @Test
    public void getReturnUrlScheme_returnsUrlSchemeBasedOnApplicationIdByDefault() {
        Context context = ApplicationProvider.getApplicationContext();
        String authorization = Fixtures.BASE64_CLIENT_TOKEN;
        BraintreeClient sut = new BraintreeClient(context, authorization);

        assertEquals("com.braintreepayments.api.test.braintree", sut.getReturnUrlScheme());
    }

    @Test
    public void getReturnUrlScheme_returnsUrlSchemeDefinedInConstructor() {
        Context context = ApplicationProvider.getApplicationContext();
        String authorization = Fixtures.BASE64_CLIENT_TOKEN;
        String returnUrlScheme = "custom-url-scheme";
        BraintreeClient sut = new BraintreeClient(context, authorization, returnUrlScheme);

        assertEquals("custom-url-scheme", sut.getReturnUrlScheme());
    }

    @Test
    public void getReturnUrlScheme_whenDefaultDeepLinkHandlerEnabled_returnsDefaultDeepLinkHandlerScheme() {
        Context context = ApplicationProvider.getApplicationContext();
        String authorization = Fixtures.BASE64_CLIENT_TOKEN;
        BraintreeClient sut = new BraintreeClient(context, authorization);
        sut.launchesBrowserSwitchAsNewTask(true);

        assertEquals("com.braintreepayments.api.test.braintree.deeplinkhandler", sut.getReturnUrlScheme());
    }

    @Test
    public void getSessionId_withAuthString_returnsSessionIdDefinedInConstructor() {
        Context context = ApplicationProvider.getApplicationContext();
        String authorization = Fixtures.BASE64_CLIENT_TOKEN;
        String sessionId = "custom-session-id";
        BraintreeClient sut = new BraintreeClient(context, authorization, sessionId, IntegrationType.DROP_IN);

        assertEquals("custom-session-id", sut.getSessionId());
    }

    @Test
    public void getSessionId_withClientTokenProvider_returnsSessionIdDefinedInConstructor() {
        Context context = ApplicationProvider.getApplicationContext();
        ClientTokenProvider clientTokenProvider = mock(ClientTokenProvider.class);
        String sessionId = "custom-session-id";
        BraintreeClient sut =
            new BraintreeClient(context, clientTokenProvider, sessionId, IntegrationType.DROP_IN);

        assertEquals("custom-session-id", sut.getSessionId());
    }

    @Test
    public void getIntegrationType_returnsCustomByDefault() {
        Context context = ApplicationProvider.getApplicationContext();
        String authorization = Fixtures.BASE64_CLIENT_TOKEN;
        BraintreeClient sut = new BraintreeClient(context, authorization);

        assertEquals("custom", sut.getIntegrationType());
    }

    @Test
    public void getIntegrationType_withAuthString_returnsIntegrationTypeDefinedInConstructor() {
        Context context = ApplicationProvider.getApplicationContext();
        String authorization = Fixtures.BASE64_CLIENT_TOKEN;
        String sessionId = "custom-session-id";
        BraintreeClient sut = new BraintreeClient(context, authorization, sessionId, IntegrationType.DROP_IN);

        assertEquals("dropin", sut.getIntegrationType());
    }

    @Test
    public void getIntegrationType_withClientTokenProvider_returnsIntegrationTypeDefinedInConstructor() {
        Context context = ApplicationProvider.getApplicationContext();
        ClientTokenProvider clientTokenProvider = mock(ClientTokenProvider.class);
        String sessionId = "custom-session-id";
        BraintreeClient sut =
            new BraintreeClient(context, clientTokenProvider, sessionId, IntegrationType.DROP_IN);

        assertEquals("dropin", sut.getIntegrationType());
    }

    @Test
    public void reportCrash_reportsCrashViaAnalyticsClient() throws JSONException {
        when(authorizationLoader.getAuthorizationFromCache()).thenReturn(authorization);

        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configuration(configuration)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, authorizationLoader);
        BraintreeClient sut = new BraintreeClient(params);

        sut.reportCrash();
        verify(analyticsClient).reportCrash(applicationContext, "session-id", IntegrationType.CUSTOM, authorization);
    }

    private BraintreeClientParams createDefaultParams(ConfigurationLoader configurationLoader, AuthorizationLoader authorizationLoader) {
        return new BraintreeClientParams()
                .authorizationLoader(authorizationLoader)
                .context(context)
                .sessionId("session-id")
                .setIntegrationType(IntegrationType.CUSTOM)
                .configurationLoader(configurationLoader)
                .httpClient(braintreeHttpClient)
                .graphQLClient(braintreeGraphQLClient)
                .analyticsClient(analyticsClient)
                .browserSwitchClient(browserSwitchClient)
                .manifestValidator(manifestValidator);
    }

    private static BrowserSwitchResult createSuccessfulBrowserSwitchResult() {
        int requestCode = 123;
        Uri url = Uri.parse("www.example.com");
        String returnUrlScheme = "sample-scheme";
        BrowserSwitchRequest browserSwitchRequest = new BrowserSwitchRequest(
                requestCode, url, new JSONObject(), returnUrlScheme, true);
        return new BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, browserSwitchRequest);
    }
}
