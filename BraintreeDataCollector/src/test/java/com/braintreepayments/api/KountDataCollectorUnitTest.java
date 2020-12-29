package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.MockBraintreeClientBuilder;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.Fixtures;
import com.kount.api.DataCollector;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class KountDataCollectorUnitTest {

    private Context context;
    private Context applicationContext;

    DataCollector kountDataCollector;

    @Before
    public void beforeEach() {
        context = mock(Context.class);
        applicationContext = mock(Context.class);
        kountDataCollector = mock(DataCollector.class);

        when(context.getApplicationContext()).thenReturn(applicationContext);
    }

    @Test
    public void getDeviceCollectorEnvironment_returnsCorrectEnvironment() {
        assertEquals(com.kount.api.DataCollector.ENVIRONMENT_PRODUCTION,
                KountDataCollector.getDeviceCollectorEnvironment("production"));
        assertEquals(com.kount.api.DataCollector.ENVIRONMENT_TEST,
                KountDataCollector.getDeviceCollectorEnvironment("sandbox"));
    }

    @Test
    public void startDataCollection_fetchesConfigurationWithApplicationContext() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .build();
        KountDataCollector sut = new KountDataCollector(braintreeClient, kountDataCollector);

        KountDataCollectorCallback callback = mock(KountDataCollectorCallback.class);
        sut.startDataCollection(context, "123", "device-session-id", callback);

        verify(braintreeClient).getConfiguration(same(applicationContext), any(ConfigurationCallback.class));
    }

    @Test
    public void startDataCollection_forwardsConfigurationFetchErrors() {
        Exception configurationError = new Exception("config error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configurationError)
                .build();
        KountDataCollector sut = new KountDataCollector(braintreeClient, kountDataCollector);

        KountDataCollectorCallback callback = mock(KountDataCollectorCallback.class);
        sut.startDataCollection(context, "123", "device-session-id", callback);

        verify(callback).onResult(null, configurationError);
    }

    @Test
    public void startDataCollection_configuresKountLibrary() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_KOUNT))
                .build();
        KountDataCollector sut = new KountDataCollector(braintreeClient, kountDataCollector);

        KountDataCollectorCallback callback = mock(KountDataCollectorCallback.class);
        sut.startDataCollection(context, "123", "device-session-id", callback);

        verify(kountDataCollector).setContext(applicationContext);
        verify(kountDataCollector).setMerchantID(123);
        verify(kountDataCollector).setLocationCollectorConfig(DataCollector.LocationConfig.COLLECT);
        verify(kountDataCollector).setEnvironment(DataCollector.ENVIRONMENT_TEST);
    }

    @Test
    public void startDataCollection_collectsKountSessionData() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_KOUNT))
                .build();
        KountDataCollector sut = new KountDataCollector(braintreeClient, kountDataCollector);

        KountDataCollectorCallback callback = mock(KountDataCollectorCallback.class);
        sut.startDataCollection(context, "123", "device-session-id", callback);

        verify(kountDataCollector).collectForSession(eq("device-session-id"), any(DataCollector.CompletionHandler.class));
    }

    @Test
    public void startDataCollection_onSuccess_forwardsSessionIdOnCompletion() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_KOUNT))
                .build();
        KountDataCollector sut = new KountDataCollector(braintreeClient, kountDataCollector);

        KountDataCollectorCallback callback = mock(KountDataCollectorCallback.class);
        sut.startDataCollection(context, "123", "device-session-id", callback);

        ArgumentCaptor<DataCollector.CompletionHandler> captor =
            ArgumentCaptor.forClass(DataCollector.CompletionHandler.class);
        verify(kountDataCollector).collectForSession(anyString(), captor.capture());

        DataCollector.CompletionHandler completionHandler = captor.getValue();
        completionHandler.completed("kount-session-id");

        verify(callback).onResult("kount-session-id", null);
    }

    @Test
    public void startDataCollection_onSuccess_sendsKountStartAnalyticsEvent() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_KOUNT))
                .build();
        KountDataCollector sut = new KountDataCollector(braintreeClient, kountDataCollector);

        KountDataCollectorCallback callback = mock(KountDataCollectorCallback.class);
        sut.startDataCollection(context, "123", "device-session-id", callback);

        ArgumentCaptor<DataCollector.CompletionHandler> captor =
                ArgumentCaptor.forClass(DataCollector.CompletionHandler.class);
        verify(kountDataCollector).collectForSession(anyString(), captor.capture());

        DataCollector.CompletionHandler completionHandler = captor.getValue();
        completionHandler.completed("kount-session-id");

        verify(braintreeClient).sendAnalyticsEvent(applicationContext, "data-collector.kount.succeeded");
    }

    @Test
    public void startDataCollection_onFailure_forwardsSessionIdOnCompletion() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_KOUNT))
                .build();
        KountDataCollector sut = new KountDataCollector(braintreeClient, kountDataCollector);

        KountDataCollectorCallback callback = mock(KountDataCollectorCallback.class);
        sut.startDataCollection(context, "123", "device-session-id", callback);

        ArgumentCaptor<DataCollector.CompletionHandler> captor =
                ArgumentCaptor.forClass(DataCollector.CompletionHandler.class);
        verify(kountDataCollector).collectForSession(anyString(), captor.capture());

        DataCollector.CompletionHandler completionHandler = captor.getValue();
        completionHandler.failed("kount-session-id", null);

        verify(callback).onResult("kount-session-id", null);
    }

    @Test
    public void startDataCollection_onFailure_sendsKountStartAnalyticsEvent() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_KOUNT))
                .build();
        KountDataCollector sut = new KountDataCollector(braintreeClient, kountDataCollector);

        KountDataCollectorCallback callback = mock(KountDataCollectorCallback.class);
        sut.startDataCollection(context, "123", "device-session-id", callback);

        ArgumentCaptor<DataCollector.CompletionHandler> captor =
                ArgumentCaptor.forClass(DataCollector.CompletionHandler.class);
        verify(kountDataCollector).collectForSession(anyString(), captor.capture());

        DataCollector.CompletionHandler completionHandler = captor.getValue();
        completionHandler.failed("kount-session-id", null);

        braintreeClient.sendAnalyticsEvent(applicationContext, "data-collector.kount.failed");
    }
}
