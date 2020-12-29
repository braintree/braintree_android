package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import com.braintreepayments.api.internal.AnalyticsDatabase;
import com.braintreepayments.api.internal.AnalyticsEvent;
import com.braintreepayments.api.internal.AnalyticsIntentService;
import com.braintreepayments.api.internal.AnalyticsSender;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.powermock.*", "org.robolectric.*", "android.*", "androidx.*" })
@PrepareForTest({ AnalyticsDatabase.class, AnalyticsSender.class })
public class AnalyticsClientTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private Context context;
    private Context applicationContext;

    private Configuration configuration;
    private AnalyticsConfiguration analyticsConfiguration;

    private AnalyticsEvent analyticsEvent;
    private AnalyticsDatabase analyticsDatabase;

    private Authorization authorization;
    private BraintreeHttpClient httpClient;

    @Before
    public void beforeEach() {
        mockStatic(AnalyticsDatabase.class);
        mockStatic(AnalyticsSender.class);

        context = mock(Context.class);
        applicationContext = mock(Context.class);

        configuration = mock(Configuration.class);
        analyticsConfiguration = mock(AnalyticsConfiguration.class);

        analyticsEvent = mock(AnalyticsEvent.class);
        analyticsDatabase = mock(AnalyticsDatabase.class);

        authorization = mock(Authorization.class);
        httpClient = mock(BraintreeHttpClient.class);
    }

    @Test
    public void sendEvent_whenAnalyticsEnabled_addsEventToAnalyticsDatabase() {
        when(context.getApplicationContext()).thenReturn(applicationContext);
        when(AnalyticsDatabase.getInstance(applicationContext)).thenReturn(analyticsDatabase);

        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);

        AnalyticsClient sut = AnalyticsClient.newInstance();
        sut.sendEvent(analyticsEvent, configuration, context);
        verify(analyticsDatabase).addEvent(analyticsEvent);
    }

    @Test
    public void sendEvent_whenAnalyticsDisabled_doesNothing() {
        when(context.getApplicationContext()).thenReturn(applicationContext);
        when(AnalyticsDatabase.getInstance(applicationContext)).thenReturn(analyticsDatabase);

        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        when(analyticsConfiguration.isEnabled()).thenReturn(false);

        AnalyticsClient sut = AnalyticsClient.newInstance();
        sut.sendEvent(analyticsEvent, configuration, context);
        verify(analyticsDatabase, never()).addEvent(analyticsEvent);
    }

    @Test
    public void sendEvent_setsLastKnownAnalyticsUrl() {
        when(context.getApplicationContext()).thenReturn(applicationContext);
        when(AnalyticsDatabase.getInstance(applicationContext)).thenReturn(analyticsDatabase);

        when(analyticsConfiguration.getUrl()).thenReturn("sample.analytics.url");
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);

        AnalyticsClient sut = AnalyticsClient.newInstance();
        sut.sendEvent(analyticsEvent, configuration, context);
        assertEquals("sample.analytics.url", sut.getLastKnownAnalyticsUrl());
    }

    @Test
    public void flushAnalyticsEvents_startsAnalyticsService() {
        when(context.getApplicationContext()).thenReturn(applicationContext);
        when(authorization.toString()).thenReturn("auth string");

        String configurationJson = "{\"config\":\"json\"}";
        when(configuration.toJson()).thenReturn(configurationJson);

        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);

        AnalyticsClient sut = AnalyticsClient.newInstance();
        sut.flushAnalyticsEvents(context, configuration, authorization, httpClient);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(applicationContext).startService(captor.capture());

        Intent intent = captor.getValue();
        assertNotNull(intent);
        assertEquals(AnalyticsIntentService.class.getName(), intent.getComponent().getClassName());
        assertEquals(intent.getStringExtra(AnalyticsIntentService.EXTRA_AUTHORIZATION), "auth string");
        assertEquals(intent.getStringExtra(AnalyticsIntentService.EXTRA_CONFIGURATION), configurationJson);
    }

    @Test
    public void flushAnalyticsEvent_whenServiceCannotStart_sendsEventUsingAnalyticsSender() {
        when(context.getApplicationContext()).thenReturn(applicationContext);
        when(authorization.toString()).thenReturn("auth string");

        String configurationJson = "{\"config\":\"json\"}";
        when(configuration.toJson()).thenReturn(configurationJson);

        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);

        when(analyticsConfiguration.getUrl()).thenReturn("/analytics/url");
        when(applicationContext.startService(any(Intent.class))).thenThrow(new RuntimeException("send failed"));

        AnalyticsClient sut = AnalyticsClient.newInstance();
        sut.flushAnalyticsEvents(context, configuration, authorization, httpClient);

        verify(applicationContext).startService(any(Intent.class));
        verifyStatic(AnalyticsSender.class, times(1));
        AnalyticsSender.send(applicationContext, authorization, configuration, httpClient, "/analytics/url", false);
    }

    @Test
    public void flushAnalyticsEvent_whenConfigurationIsNull_doesNothing() {
        AnalyticsClient sut = AnalyticsClient.newInstance();
        sut.flushAnalyticsEvents(context, null, authorization, httpClient);
        verify(applicationContext, never()).startService(any(Intent.class));
    }

    @Test
    public void flushAnalyticsEvent_whenConfigurationToJsonIsNull_doesNothing() {
        when(configuration.toJson()).thenReturn(null);

        AnalyticsClient sut = AnalyticsClient.newInstance();
        sut.flushAnalyticsEvents(context, null, authorization, httpClient);

        verify(applicationContext, never()).startService(any(Intent.class));
    }

    @Test
    public void flushAnalyticsEvent_whenAnalyticsIsDisabled_doesNothing() {
        String configurationJson = "{\"config\":\"json\"}";
        when(configuration.toJson()).thenReturn(configurationJson);

        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        when(analyticsConfiguration.isEnabled()).thenReturn(false);

        AnalyticsClient sut = AnalyticsClient.newInstance();
        sut.flushAnalyticsEvents(context, null, authorization, httpClient);

        verify(applicationContext, never()).startService(any(Intent.class));
    }
}