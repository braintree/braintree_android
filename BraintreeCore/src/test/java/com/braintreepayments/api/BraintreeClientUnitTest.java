package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class BraintreeClientUnitTest {

    private Authorization authorization;
    private Context context;
    private Context applicationContext;

    private BraintreeHttpClient braintreeHttpClient;
    private BraintreeGraphQLHttpClient braintreeGraphQLHttpClient;
    private ConfigurationLoader configurationLoader;
    private AnalyticsClient analyticsClient;
    private ManifestValidator manifestValidator;
    private BrowserSwitchClient browserSwitchClient;

    @Before
    public void beforeEach() {
        authorization = mock(Authorization.class);
        context = mock(Context.class);
        applicationContext = ApplicationProvider.getApplicationContext();

        braintreeHttpClient = mock(BraintreeHttpClient.class);
        braintreeGraphQLHttpClient = mock(BraintreeGraphQLHttpClient.class);
        configurationLoader = mock(ConfigurationLoader.class);
        analyticsClient = mock(AnalyticsClient.class);
        manifestValidator = mock(ManifestValidator.class);
        browserSwitchClient = mock(BrowserSwitchClient.class);

        when(context.getApplicationContext()).thenReturn(applicationContext);
    }

    @Test
    public void getConfiguration_onSuccess_forwardsInvocationToConfigurationLoader() {
        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        ConfigurationCallback configurationCallback = mock(ConfigurationCallback.class);
        sut.getConfiguration(configurationCallback);

        verify(configurationLoader).loadConfiguration(same(applicationContext), same(authorization), same(configurationCallback));
    }

    @Test
    public void sendGET_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        Configuration configuration = mock(Configuration.class);
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configuration(configuration)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGET("sample-url", httpResponseCallback);

        verify(braintreeHttpClient).get(eq("sample-url"), same(configuration), same(httpResponseCallback));
    }

    @Test
    public void sendGET_onGetConfigurationFailure_forwardsErrorToCallback() {
        Exception exception = new Exception("configuration error");
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configurationError(exception)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGET("sample-url", httpResponseCallback);

        verify(httpResponseCallback).failure(same(exception));
    }

    @Test
    public void sendPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        Configuration configuration = mock(Configuration.class);
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configuration(configuration)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendPOST("sample-url", "{}", httpResponseCallback);

        verify(braintreeHttpClient).post(eq("sample-url"), eq("{}"), same(configuration), same(httpResponseCallback));
    }

    @Test
    public void sendPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        Exception exception = new Exception("configuration error");
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configurationError(exception)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendPOST("sample-url", "{}", httpResponseCallback);

        verify(httpResponseCallback).failure(same(exception));
    }

    @Test
    public void sendGraphQLPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        Configuration configuration = mock(Configuration.class);
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configuration(configuration)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGraphQLPOST("{}", httpResponseCallback);

        verify(braintreeGraphQLHttpClient).post(eq("{}"), same(configuration), same(httpResponseCallback));
    }

    @Test
    public void sendGraphQLPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        Exception exception = new Exception("configuration error");
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configurationError(exception)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGraphQLPOST("{}", httpResponseCallback);

        verify(httpResponseCallback).failure(same(exception));
    }

    @Test
    public void sendAnalyticsEvent_sendsEventToAnalyticsClient() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configuration(configuration)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);
        sut.sendAnalyticsEvent("event.started");

        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsClient).sendEvent(same(applicationContext), same(configuration), captor.capture());

        AnalyticsEvent event = captor.getValue();
        assertEquals("sessionId", event.metadata.getString("sessionId"));
        assertEquals("integrationType", event.metadata.getString("integrationType"));
        assertEquals("android.event.started", event.event);
    }

    @Test
    public void sendAnalyticsEvent_whenConfigurationLoadFails_doesNothing() {
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configurationError(new Exception("error"))
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);
        sut.sendAnalyticsEvent("event.started");

        verifyZeroInteractions(analyticsClient);
    }

    @Test
    public void sendAnalyticsEvent_whenAnalyticsConfigurationNull_doesNothing() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.isAnalyticsEnabled()).thenReturn(false);
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configuration(configuration)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);
        sut.sendAnalyticsEvent("event.started");

        verifyZeroInteractions(analyticsClient);
    }

    @Test
    public void sendAnalyticsEvent_whenAnalyticsNotEnabled_doesNothing() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ANALYTICS);
        ConfigurationLoader configurationLoader = new MockConfigurationLoaderBuilder()
                .configuration(configuration)
                .build();

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);
        sut.sendAnalyticsEvent("event.started");

        verifyZeroInteractions(analyticsClient);
    }

    @Test
    public void getSessionId_forwardsSessionIdFromInstantiation() {
        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        assertEquals("sessionId", sut.getSessionId());
    }

    @Test
    public void getIntegrationType_forwardsIntegrationTypeFromInstantiation() {
        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        assertEquals("integrationType", sut.getIntegrationType());
    }

    @Test
    public void startBrowserSwitch_forwardsInvocationToBrowserSwitchClient() throws BrowserSwitchException {
        FragmentActivity activity = mock(FragmentActivity.class);
        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions();

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        sut.startBrowserSwitch(activity, browserSwitchOptions);
        verify(browserSwitchClient).start(activity, browserSwitchOptions);
    }

    @Test
    public void deliverBrowserSwitchResult_forwardsInvocationToBrowserSwitchClient() {
        FragmentActivity activity = mock(FragmentActivity.class);

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        sut.deliverBrowserSwitchResult(activity);
        verify(browserSwitchClient).deliverResult(activity);
    }

    @Test
    public void canPerformBrowserSwitch_assertsBrowserSwitchIsPossible() throws BrowserSwitchException {
        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
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

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        assertTrue(sut.canPerformBrowserSwitch(activity, 123));
    }

    @Test
    public void canPerformBrowserSwitch_onError_returnsFalse() throws BrowserSwitchException {
        FragmentActivity activity = mock(FragmentActivity.class);
        doThrow(new BrowserSwitchException("error")).when(browserSwitchClient).assertCanPerformBrowserSwitch(same(activity), any(BrowserSwitchOptions.class));

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        sut.canPerformBrowserSwitch(activity, 123);
        assertFalse(sut.canPerformBrowserSwitch(activity, 123));
    }

    @Test
    public void isUrlSchemeDeclaredInAndroidManifest_forwardsInvocationToManifestValidator() {
        when(manifestValidator.isUrlSchemeDeclaredInAndroidManifest(applicationContext, "a-url-scheme", FragmentActivity.class)).thenReturn(true);

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        assertTrue(sut.isUrlSchemeDeclaredInAndroidManifest("a-url-scheme", FragmentActivity.class));
    }

    @Test
    public void getManifestActivityInfo_forwardsInvocationToManifestValidator() {
        ActivityInfo activityInfo = new ActivityInfo();
        when(manifestValidator.getActivityInfo(applicationContext, FragmentActivity.class)).thenReturn(activityInfo);

        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        assertSame(activityInfo, sut.getManifestActivityInfo(FragmentActivity.class));
    }

    @Test
    public void getReturnUrlScheme_returnsUrlScheme() {
        BraintreeClientParams params = createDefaultParams(configurationLoader, "sessionId", "integrationType");
        BraintreeClient sut = new BraintreeClient(params);

        String returnUrlScheme = sut.getReturnUrlScheme();
        assertEquals("com.braintreepayments.api.test.braintree", returnUrlScheme);
    }

    private BraintreeClientParams createDefaultParams(ConfigurationLoader configurationLoader, String sessionId, String integrationType) {
        return new BraintreeClientParams()
                .authorization(authorization)
                .context(context)
                .sessionId(sessionId)
                .setIntegrationType(integrationType)
                .configurationLoader(configurationLoader)
                .httpClient(braintreeHttpClient)
                .graphQLHttpClient(braintreeGraphQLHttpClient)
                .analyticsClient(analyticsClient)
                .browserSwitchClient(browserSwitchClient)
                .manifestValidator(manifestValidator);
    }
}
