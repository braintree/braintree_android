package com.braintreepayments.api;

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

import static com.braintreepayments.api.SharedPreferencesHelper.getSharedPreferences;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ConfigurationCacheUnitTest {

    BraintreeSharedPreferences braintreeSharedPreferences;
    Context context;

    @Before
    public void beforeEach() throws GeneralSecurityException, IOException {
        context = ApplicationProvider.getApplicationContext();
        braintreeSharedPreferences = mock(BraintreeSharedPreferences.class);
        when(braintreeSharedPreferences.getSharedPreferences(context)).thenReturn(getSharedPreferences(context));
    }

    @Test
    public void saveConfiguration_savesConfigurationInSharedPrefs() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        ConfigurationCache sut = new ConfigurationCache();
        sut.saveConfiguration(context, configuration, braintreeSharedPreferences, "cacheKey", 123);

        assertEquals(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN, getSharedPreferences(context).getString("cacheKey", ""));
        assertEquals(123L, getSharedPreferences(context).getLong("cacheKey_timestamp", 0));
    }

    @Test
    public void getCacheConfiguration_returnsConfigurationFromSharedPrefs() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        ConfigurationCache sut = new ConfigurationCache();
        sut.saveConfiguration(context, configuration, braintreeSharedPreferences, "cacheKey", 0);

        assertEquals(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN, ConfigurationCache.getInstance().getConfiguration(context, braintreeSharedPreferences, "cacheKey", TimeUnit.MINUTES.toMillis(5)-1));
    }

    @Test
    public void getCacheConfiguration_returnsNullIfCacheEntryExpires() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        ConfigurationCache sut = new ConfigurationCache();
        sut.saveConfiguration(context, configuration, braintreeSharedPreferences, "cacheKey", 0);

        assertNull(ConfigurationCache.getInstance().getConfiguration(context, braintreeSharedPreferences, "cacheKey", TimeUnit.MINUTES.toMillis(5)));
    }
}