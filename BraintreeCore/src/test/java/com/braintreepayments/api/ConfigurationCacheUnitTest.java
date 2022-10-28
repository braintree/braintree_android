package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class ConfigurationCacheUnitTest {

    BraintreeSharedPreferences braintreeSharedPreferences;
    Context context;

    @Before
    public void beforeEach() throws GeneralSecurityException, IOException {
        context = ApplicationProvider.getApplicationContext();
        braintreeSharedPreferences = mock(BraintreeSharedPreferences.class);
    }

    @Test
    public void saveConfiguration_savesConfigurationInSharedPrefs() throws JSONException, UnexpectedException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        ConfigurationCache sut = new ConfigurationCache(braintreeSharedPreferences);
        sut.saveConfiguration(configuration, "cacheKey", 123L);

        verify(braintreeSharedPreferences).putStringAndLong("cacheKey", configuration.toJson(), "cacheKey_timestamp", 123L);
    }

    @Test(expected = UnexpectedException.class)
    public void saveConfiguration_whenSharedPreferencesFails_forwardsException() throws JSONException, UnexpectedException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        ConfigurationCache sut = new ConfigurationCache(braintreeSharedPreferences);

        UnexpectedException unexpectedException = new UnexpectedException("unexpected exception");
        doThrow(unexpectedException)
                .when(braintreeSharedPreferences)
                .putStringAndLong(anyString(), anyString(), anyString(), anyLong());

        sut.saveConfiguration(configuration, "cacheKey", 123);
    }

    @Test
    public void getConfiguration_returnsConfigurationFromSharedPrefs() throws JSONException, UnexpectedException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        when(braintreeSharedPreferences.containsKey("cacheKey_timestamp")).thenReturn(true);
        when(braintreeSharedPreferences.getLong("cacheKey_timestamp")).thenReturn(0L);
        when(braintreeSharedPreferences.getString("cacheKey", "")).thenReturn(configuration.toJson());

        ConfigurationCache sut = new ConfigurationCache(braintreeSharedPreferences);
        sut.saveConfiguration(configuration, "cacheKey", 0);

        assertEquals(configuration.toJson(), sut.getConfiguration("cacheKey", TimeUnit.MINUTES.toMillis(5) - 1));
    }

    @Test
    public void getConfiguration_whenCacheEntryExpires_returnsNull() throws JSONException, UnexpectedException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        when(braintreeSharedPreferences.containsKey("cacheKey_timestamp")).thenReturn(true);
        when(braintreeSharedPreferences.getLong("cacheKey_timestamp")).thenReturn(TimeUnit.MINUTES.toMillis(5));
        when(braintreeSharedPreferences.getString("cacheKey", "")).thenReturn(configuration.toJson());

        ConfigurationCache sut = new ConfigurationCache(braintreeSharedPreferences);
        sut.saveConfiguration(configuration, "cacheKey", 0);

        assertNull(sut.getConfiguration("cacheKey", TimeUnit.MINUTES.toMillis(20)));
    }

    @Test(expected = UnexpectedException.class)
    public void getConfiguration_whenSharedPreferencesFails_forwardsException() throws UnexpectedException {
        UnexpectedException unexpectedException = new UnexpectedException("unexpected exception");
        when(braintreeSharedPreferences.containsKey(anyString())).thenThrow(unexpectedException);
        when(braintreeSharedPreferences.getLong(anyString())).thenThrow(unexpectedException);
        when(braintreeSharedPreferences.getString(anyString(), anyString())).thenThrow(unexpectedException);

        ConfigurationCache sut = new ConfigurationCache(braintreeSharedPreferences);
        assertNull(sut.getConfiguration("cacheKey", TimeUnit.MINUTES.toMillis(5) - 1));
    }
}