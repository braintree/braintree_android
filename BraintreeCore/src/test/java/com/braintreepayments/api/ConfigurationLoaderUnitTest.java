package com.braintreepayments.api;

import android.content.Context;
import android.util.Base64;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.powermock.*", "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
public class ConfigurationLoaderUnitTest {

    private ConfigurationCache configurationCache;

    private BraintreeHttpClient braintreeHttpClient;
    private ConfigurationCallback callback;

    private Context context;
    private Authorization authorization;

    @Before
    public void beforeEach() {
        configurationCache = mock(ConfigurationCache.class);

        authorization = mock(Authorization.class);
        context = mock(Context.class);

        braintreeHttpClient = mock(BraintreeHttpClient.class);
        callback = mock(ConfigurationCallback.class);
    }

    @Test
    public void loadConfiguration_loadsConfigurationForTheCurrentEnvironment() {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient, configurationCache);
        sut.loadConfiguration(context, authorization, callback);

        String expectedConfigUrl = "https://example.com/config?configVersion=3";
        ArgumentCaptor<HttpResponseCallback> captor = ArgumentCaptor.forClass(HttpResponseCallback.class);

        verify(braintreeHttpClient).get(eq(expectedConfigUrl), (Configuration) isNull(), eq(HttpClient.RETRY_MAX_3_TIMES), captor.capture(), same(authorization));

        HttpResponseCallback httpResponseCallback = captor.getValue();
        httpResponseCallback.success(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN);

        verify(callback).onResult(any(Configuration.class), (Exception) isNull());
    }

    @Test
    public void loadConfiguration_savesFetchedConfigurationToCache() {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");
        when(authorization.getBearer()).thenReturn("bearer");

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient, configurationCache);
        sut.loadConfiguration(context, authorization, callback);

        String expectedConfigUrl = "https://example.com/config?configVersion=3";
        ArgumentCaptor<HttpResponseCallback> captor = ArgumentCaptor.forClass(HttpResponseCallback.class);

        verify(braintreeHttpClient).get(eq(expectedConfigUrl), (Configuration) isNull(), eq(HttpClient.RETRY_MAX_3_TIMES), captor.capture(), same(authorization));

        HttpResponseCallback httpResponseCallback = captor.getValue();
        httpResponseCallback.success(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN);

        String cacheKey = Base64.encodeToString(String.format("%s%s", "https://example.com/config?configVersion=3", "bearer").getBytes(), 0);
        verify(configurationCache).saveConfiguration(same(context), any(Configuration.class), eq(cacheKey));
    }

    @Test
    public void loadConfiguration_onJSONParsingError_forwardsExceptionToErrorResponseListener() {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient, configurationCache);
        sut.loadConfiguration(context, authorization, callback);

        ArgumentCaptor<HttpResponseCallback> captor = ArgumentCaptor.forClass(HttpResponseCallback.class);
        verify(braintreeHttpClient).get(anyString(), (Configuration) isNull(), eq(HttpClient.RETRY_MAX_3_TIMES), captor.capture(), same(authorization));

        HttpResponseCallback httpResponseCallback = captor.getValue();
        httpResponseCallback.success("not json");

        verify(callback).onResult((Configuration) isNull(), any(JSONException.class));
    }

    @Test
    public void loadConfiguration_onHttpError_forwardsExceptionToErrorResponseListener() {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient, configurationCache);
        sut.loadConfiguration(context, authorization, callback);

        ArgumentCaptor<HttpResponseCallback> httpResponseCaptor = ArgumentCaptor.forClass(HttpResponseCallback.class);
        verify(braintreeHttpClient).get(anyString(), (Configuration) isNull(), eq(HttpClient.RETRY_MAX_3_TIMES), httpResponseCaptor.capture(), same(authorization));

        HttpResponseCallback httpResponseCallback = httpResponseCaptor.getValue();
        Exception httpError = new Exception("http error");
        httpResponseCallback.failure(httpError);

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((Configuration) isNull(), errorCaptor.capture());

        ConfigurationException error = (ConfigurationException) errorCaptor.getValue();
        assertEquals("Request for configuration has failed: http error",
                error.getMessage());
    }

    @Test
    public void loadConfiguration_whenCachedConfigurationAvailable_loadsConfigurationFromCache() {
        String cacheKey = Base64.encodeToString(String.format("%s%s", "https://example.com/config?configVersion=3", "bearer").getBytes(), 0);
        Context context = mock(Context.class);

        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");
        when(authorization.getBearer()).thenReturn("bearer");
        when(configurationCache.getConfiguration(context, cacheKey)).thenReturn(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN);

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient, configurationCache);
        sut.loadConfiguration(context, authorization, callback);

        verify(braintreeHttpClient, times(0)).get(anyString(), (Configuration) isNull(), anyInt(), any(HttpResponseCallback.class), same(authorization));
        verify(callback).onResult(any(Configuration.class), (Exception) isNull());
    }
}