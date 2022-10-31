package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.powermock.*", "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
public class ConfigurationLoaderUnitTest {

    private ConfigurationCache configurationCache;

    private BraintreeHttpClient braintreeHttpClient;
    private ConfigurationLoaderCallback callback;

    private Authorization authorization;

    @Before
    public void beforeEach() {
        configurationCache = mock(ConfigurationCache.class);
        authorization = mock(Authorization.class);

        braintreeHttpClient = mock(BraintreeHttpClient.class);
        callback = mock(ConfigurationLoaderCallback.class);
    }

    @Test
    public void loadConfiguration_loadsConfigurationForTheCurrentEnvironment() {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient, configurationCache);
        sut.loadConfiguration(authorization, callback);

        String expectedConfigUrl = "https://example.com/config?configVersion=3";
        ArgumentCaptor<HttpResponseCallback> captor = ArgumentCaptor.forClass(HttpResponseCallback.class);

        verify(braintreeHttpClient).get(eq(expectedConfigUrl), (Configuration) isNull(), same(authorization), eq(HttpClient.RETRY_MAX_3_TIMES), captor.capture());

        HttpResponseCallback httpResponseCallback = captor.getValue();
        httpResponseCallback.onResult(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN, null);

        verify(callback).onResult(any(ConfigurationLoaderResult.class), (Exception) isNull());
    }

    @Test
    public void loadConfiguration_savesFetchedConfigurationToCache() throws BraintreeSharedPreferencesException {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");
        when(authorization.getBearer()).thenReturn("bearer");

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient, configurationCache);
        sut.loadConfiguration(authorization, callback);

        String expectedConfigUrl = "https://example.com/config?configVersion=3";
        ArgumentCaptor<HttpResponseCallback> captor = ArgumentCaptor.forClass(HttpResponseCallback.class);

        verify(braintreeHttpClient).get(eq(expectedConfigUrl), (Configuration) isNull(), same(authorization), eq(HttpClient.RETRY_MAX_3_TIMES), captor.capture());

        HttpResponseCallback httpResponseCallback = captor.getValue();
        httpResponseCallback.onResult(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN, null);

        String cacheKey = Base64.encodeToString(String.format("%s%s", "https://example.com/config?configVersion=3", "bearer").getBytes(), 0);
        verify(configurationCache).saveConfiguration(any(Configuration.class), eq(cacheKey));
    }

    @Test
    public void loadConfiguration_onJSONParsingError_forwardsExceptionToErrorResponseListener() {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient, configurationCache);
        sut.loadConfiguration(authorization, callback);

        ArgumentCaptor<HttpResponseCallback> captor = ArgumentCaptor.forClass(HttpResponseCallback.class);
        verify(braintreeHttpClient).get(anyString(), (Configuration) isNull(), same(authorization), eq(HttpClient.RETRY_MAX_3_TIMES), captor.capture());

        HttpResponseCallback httpResponseCallback = captor.getValue();
        httpResponseCallback.onResult("not json", null);

        verify(callback).onResult((ConfigurationLoaderResult) isNull(), any(JSONException.class));
    }

    @Test
    public void loadConfiguration_onHttpError_forwardsExceptionToErrorResponseListener() {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient, configurationCache);
        sut.loadConfiguration(authorization, callback);

        ArgumentCaptor<HttpResponseCallback> httpResponseCaptor = ArgumentCaptor.forClass(HttpResponseCallback.class);
        verify(braintreeHttpClient).get(anyString(), (Configuration) isNull(), same(authorization), eq(HttpClient.RETRY_MAX_3_TIMES), httpResponseCaptor.capture());

        HttpResponseCallback httpResponseCallback = httpResponseCaptor.getValue();
        Exception httpError = new Exception("http error");
        httpResponseCallback.onResult(null, httpError);

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((ConfigurationLoaderResult) isNull(), errorCaptor.capture());

        ConfigurationException error = (ConfigurationException) errorCaptor.getValue();
        assertEquals("Request for configuration has failed: http error",
                error.getMessage());
    }

    @Test
    public void loadConfiguration_whenInvalidToken_forwardsExceptionToCallback() {
        Authorization authorization = new InvalidAuthorization("invalid", "token invalid");

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient, configurationCache);
        sut.loadConfiguration(authorization, callback);

        ArgumentCaptor<BraintreeException> captor = ArgumentCaptor.forClass(BraintreeException.class);
        verify(callback).onResult((ConfigurationLoaderResult) isNull(), captor.capture());

        BraintreeException exception = captor.getValue();
        assertEquals("token invalid", exception.getMessage());
    }

    @Test
    public void loadConfiguration_whenCachedConfigurationAvailable_loadsConfigurationFromCache() throws BraintreeSharedPreferencesException {
        String cacheKey = Base64.encodeToString(String.format("%s%s", "https://example.com/config?configVersion=3", "bearer").getBytes(), 0);

        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");
        when(authorization.getBearer()).thenReturn("bearer");
        when(configurationCache.getConfiguration(cacheKey)).thenReturn(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN);

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient, configurationCache);
        sut.loadConfiguration(authorization, callback);

        verify(braintreeHttpClient, times(0)).get(anyString(), (Configuration) isNull(), same(authorization), anyInt(), any(HttpResponseCallback.class));
        verify(callback).onResult(any(ConfigurationLoaderResult.class), (Exception) isNull());
    }

    @Test
    public void loadConfiguration_forwardsConfigurationCacheErrors() throws BraintreeSharedPreferencesException, JSONException {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");
        when(authorization.getBearer()).thenReturn("bearer");

        BraintreeSharedPreferencesException cacheLoadError =
            new BraintreeSharedPreferencesException("cache load error");
        when(configurationCache.getConfiguration(anyString()))
                .thenThrow(cacheLoadError);

        BraintreeSharedPreferencesException cacheSaveError =
            new BraintreeSharedPreferencesException("cache save error");
        doThrow(cacheSaveError)
                .when(configurationCache).saveConfiguration(any(Configuration.class), anyString());

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient, configurationCache);
        sut.loadConfiguration(authorization, callback);

        String expectedConfigUrl = "https://example.com/config?configVersion=3";
        ArgumentCaptor<HttpResponseCallback> captor = ArgumentCaptor.forClass(HttpResponseCallback.class);

        verify(braintreeHttpClient).get(eq(expectedConfigUrl), (Configuration) isNull(), same(authorization), eq(HttpClient.RETRY_MAX_3_TIMES), captor.capture());

        HttpResponseCallback httpResponseCallback = captor.getValue();
        httpResponseCallback.onResult(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN, null);

        ArgumentCaptor<ConfigurationLoaderResult> resultCaptor =
                ArgumentCaptor.forClass(ConfigurationLoaderResult.class);

        verify(callback).onResult(resultCaptor.capture(), (Exception) isNull());

        ConfigurationLoaderResult result = resultCaptor.getValue();

        JSONObject expectedConfig = new JSONObject(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN);
        JSONObject actualConfig = new JSONObject(result.getConfiguration().toJson());
        JSONAssert.assertEquals(expectedConfig, actualConfig, true);

        assertSame(cacheLoadError, result.getLoadFromCacheError());
        assertSame(cacheSaveError, result.getSaveToCacheError());
    }
}
