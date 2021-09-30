package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
    public void saveConfiguration_savesConfigurationInSharedPrefs() throws JSONException, GeneralSecurityException, IOException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        ConfigurationCache sut = new ConfigurationCache();
        sut.saveConfiguration(context, configuration, braintreeSharedPreferences, "cacheKey", 123);
        verify(braintreeSharedPreferences).putStringAndLong(context, "cacheKey", configuration.toJson(), "cacheKey_timestamp", 123L);
    }

    @Test
    public void getCacheConfiguration_returnsConfigurationFromSharedPrefs() throws JSONException, GeneralSecurityException, IOException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        when(braintreeSharedPreferences.containsKey(context, "cacheKey_timestamp")).thenReturn(true);
        when(braintreeSharedPreferences.getLong(context, "cacheKey_timestamp")).thenReturn(0L);
        when(braintreeSharedPreferences.getString(context, "cacheKey")).thenReturn(configuration.toJson());

        ConfigurationCache sut = new ConfigurationCache();
        sut.saveConfiguration(context, configuration, braintreeSharedPreferences, "cacheKey", 0);

        assertEquals(configuration.toJson(), ConfigurationCache.getInstance().getConfiguration(context, braintreeSharedPreferences, "cacheKey", TimeUnit.MINUTES.toMillis(5)-1));
    }

    @Test
    public void getCacheConfiguration_returnsNullIfCacheEntryExpires() throws JSONException, GeneralSecurityException, IOException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        when(braintreeSharedPreferences.containsKey(context, "cacheKey_timestamp")).thenReturn(true);
        when(braintreeSharedPreferences.getLong(context, "cacheKey_timestamp")).thenReturn(TimeUnit.MINUTES.toMillis(5));
        when(braintreeSharedPreferences.getString(context, "cacheKey")).thenReturn(configuration.toJson());

        ConfigurationCache sut = new ConfigurationCache();
        sut.saveConfiguration(context, configuration, braintreeSharedPreferences, "cacheKey", 0);

        assertNull(ConfigurationCache.getInstance().getConfiguration(context, braintreeSharedPreferences, "cacheKey", TimeUnit.MINUTES.toMillis(20)));
    }
}