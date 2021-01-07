package com.braintreepayments.api;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeGraphQLHttpClient;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.Fixtures;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

// TODO: Complete unit tests
@RunWith(RobolectricTestRunner.class)
public class BraintreeClientUnitTest {

    private Authorization authorization;
    private Context context;
    private Context applicationContext;

    private BraintreeHttpClient braintreeHttpClient;
    private BraintreeGraphQLHttpClient braintreeGraphQLHttpClient;
    private ConfigurationManager configurationManager;
    private AnalyticsClient analyticsClient;

    @Before
    public void beforeEach() {
        authorization = mock(Authorization.class);
        context = mock(Context.class);
        applicationContext = ApplicationProvider.getApplicationContext();

        braintreeHttpClient = mock(BraintreeHttpClient.class);
        braintreeGraphQLHttpClient = mock(BraintreeGraphQLHttpClient.class);
        configurationManager = mock(ConfigurationManager.class);
        analyticsClient = mock(AnalyticsClient.class);

        when(context.getApplicationContext()).thenReturn(applicationContext);
    }

    @Test
    public void getConfiguration_onSuccess_forwardsInvocationToConfigurationLoader() {
        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager, analyticsClient, "sessionId");

        ConfigurationCallback configurationCallback = mock(ConfigurationCallback.class);
        sut.getConfiguration(configurationCallback);

        verify(configurationManager).loadConfiguration(same(applicationContext), same(authorization), same(configurationCallback));
    }

    @Test
    public void sendGET_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        Configuration configuration = mock(Configuration.class);
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configuration(configuration)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager, analyticsClient, "sessionId");

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGET("sample-url", httpResponseCallback);

        verify(braintreeHttpClient).get(eq("sample-url"), same(configuration), same(httpResponseCallback));
    }

    @Test
    public void sendGET_onGetConfigurationFailure_forwardsErrorToCallback() {
        Exception exception = new Exception("configuration error");
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configurationError(exception)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager, analyticsClient, "sessionId");

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGET("sample-url", httpResponseCallback);

        verify(httpResponseCallback).failure(same(exception));
    }

    @Test
    public void sendPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        Configuration configuration = mock(Configuration.class);
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configuration(configuration)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager, analyticsClient, "sessionId");

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendPOST("sample-url", "{}", httpResponseCallback);

        verify(braintreeHttpClient).post(eq("sample-url"), eq("{}"), same(configuration), same(httpResponseCallback));
    }

    @Test
    public void sendPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        Exception exception = new Exception("configuration error");
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configurationError(exception)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager, analyticsClient, "sessionId");

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendPOST("sample-url", "{}", httpResponseCallback);

        verify(httpResponseCallback).failure(same(exception));
    }

    @Test
    public void sendGraphQLPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        Configuration configuration = mock(Configuration.class);
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configuration(configuration)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager, analyticsClient, "sessionId");

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGraphQLPOST("{}", httpResponseCallback);

        verify(braintreeGraphQLHttpClient).post(eq("{}"), same(configuration), same(httpResponseCallback));
    }

    @Test
    public void sendGraphQLPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        Exception exception = new Exception("configuration error");
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configurationError(exception)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager, analyticsClient, "sessionId");

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGraphQLPOST("{}", httpResponseCallback);

        verify(httpResponseCallback).failure(same(exception));
    }

    @Test
    public void sendAnalyticsEvent_sendsEventToAnalyticsClient() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configuration(configuration)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager, analyticsClient, "sessionId");
        sut.sendAnalyticsEvent("event.started");

        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsClient).sendEvent(captor.capture(), same(configuration), same(applicationContext));

        AnalyticsEvent event = captor.getValue();
        assertEquals("sessionId", event.metadata.getString("sessionId"));
        assertEquals("custom", event.metadata.getString("integrationType"));
        assertEquals("android.event.started", event.event);
    }

    @Test
    public void sendAnalyticsEvent_whenConfigurationLoadFails_doesNothing() {
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configurationError(new Exception("error"))
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager, analyticsClient, "sessionId");
        sut.sendAnalyticsEvent("event.started");

        verifyZeroInteractions(analyticsClient);
    }

    @Test
    public void sendAnalyticsEvent_whenAnalyticsConfigurationNull_doesNothing() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(null);
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configuration(configuration)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager, analyticsClient, "sessionId");
        sut.sendAnalyticsEvent("event.started");

        verifyZeroInteractions(analyticsClient);
    }

    @Test
    public void sendAnalyticsEvent_whenAnalyticsNotEnabled_doesNothing() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ANALYTICS);
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configuration(configuration)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager, analyticsClient, "sessionId");
        sut.sendAnalyticsEvent("event.started");

        verifyZeroInteractions(analyticsClient);
    }
}
