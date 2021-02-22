package com.braintreepayments.api;

import android.content.Context;
import android.util.Base64;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.powermock.*", "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest({Configuration.class, ConfigurationCache.class})
public class ConfigurationLoaderUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private Configuration configuration;
    private BraintreeHttpClient braintreeHttpClient;
    private ConfigurationCallback callback;

    private Context context;
    private Authorization authorization;

    @Before
    public void beforeEach() {
        mockStatic(Configuration.class, ConfigurationCache.class);

        authorization = mock(Authorization.class);
        configuration = mock(Configuration.class);
        context = mock(Context.class);

        braintreeHttpClient = mock(BraintreeHttpClient.class);
        callback = mock(ConfigurationCallback.class);
    }

    @Test
    public void loadConfiguration_loadsConfigurationForTheCurrentEnvironment() throws JSONException {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");
        when(Configuration.fromJson("{}")).thenReturn(configuration);

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient);
        sut.loadConfiguration(context, authorization, callback);

        String expectedConfigUrl = "https://example.com/config?configVersion=3";
        ArgumentCaptor<HttpResponseCallback> captor = ArgumentCaptor.forClass(HttpResponseCallback.class);

        verify(braintreeHttpClient).get(eq(expectedConfigUrl), (Configuration) isNull(), eq(HttpClient.RETRY_MAX_3_TIMES), captor.capture());

        HttpResponseCallback httpResponseCallback = captor.getValue();
        httpResponseCallback.success("{}");

        verify(callback).onResult(configuration, null);
    }

    @Test
    public void loadConfiguration_savesFetchedConfigurationToCache() throws JSONException {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");
        when(authorization.getBearer()).thenReturn("bearer");
        when(Configuration.fromJson("{}")).thenReturn(configuration);

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient);
        sut.loadConfiguration(context, authorization, callback);

        String expectedConfigUrl = "https://example.com/config?configVersion=3";
        ArgumentCaptor<HttpResponseCallback> captor = ArgumentCaptor.forClass(HttpResponseCallback.class);

        verify(braintreeHttpClient).get(eq(expectedConfigUrl), (Configuration) isNull(), eq(HttpClient.RETRY_MAX_3_TIMES), captor.capture());

        HttpResponseCallback httpResponseCallback = captor.getValue();
        httpResponseCallback.success("{}");

        verifyStatic(ConfigurationCache.class);
        String cacheKey = Base64.encodeToString(String.format("%s%s", "https://example.com/config?configVersion=3", "bearer").getBytes(), 0);
        ConfigurationCache.saveConfiguration(context, configuration, cacheKey);
    }

    @Test
    public void loadConfiguration_onJSONParsingError_forwardsExceptionToErrorResponseListener() throws JSONException {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");

        JSONException jsonException = new JSONException("json message");
        when(Configuration.fromJson("not json")).thenThrow(jsonException);

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient);
        sut.loadConfiguration(context, authorization, callback);

        ArgumentCaptor<HttpResponseCallback> captor = ArgumentCaptor.forClass(HttpResponseCallback.class);
        verify(braintreeHttpClient).get(anyString(), (Configuration) isNull(), eq(HttpClient.RETRY_MAX_3_TIMES), captor.capture());

        HttpResponseCallback httpResponseCallback = captor.getValue();
        httpResponseCallback.success("not json");

        verify(callback).onResult(null, jsonException);
    }

    @Test
    public void loadConfiguration_onHttpError_forwardsExceptionToErrorResponseListener() {
        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient);
        sut.loadConfiguration(context, authorization, callback);

        ArgumentCaptor<HttpResponseCallback> httpResponseCaptor = ArgumentCaptor.forClass(HttpResponseCallback.class);
        verify(braintreeHttpClient).get(anyString(), (Configuration) isNull(), eq(HttpClient.RETRY_MAX_3_TIMES), httpResponseCaptor.capture());

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
    public void loadConfiguration_whenCachedConfigurationAvailable_loadsConfigurationFromCache() throws JSONException {
        String cacheKey = Base64.encodeToString(String.format("%s%s", "https://example.com/config?configVersion=3", "bearer").getBytes(), 0);
        Context context = mock(Context.class);

        when(authorization.getConfigUrl()).thenReturn("https://example.com/config");
        when(authorization.getBearer()).thenReturn("bearer");
        when(ConfigurationCache.getConfiguration(context, cacheKey)).thenReturn("{}");
        when(Configuration.fromJson("{}")).thenReturn(configuration);

        ConfigurationLoader sut = new ConfigurationLoader(braintreeHttpClient);
        sut.loadConfiguration(context, authorization, callback);

        verify(braintreeHttpClient, times(0)).get(anyString(), (Configuration) isNull(), anyInt(), any(HttpResponseCallback.class));
        verify(callback).onResult(configuration, null);
    }
}