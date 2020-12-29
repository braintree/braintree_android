package com.braintreepayments.api;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.Fixtures;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.TimeUnit;

import static com.braintreepayments.testutils.SharedPreferencesHelper.getSharedPreferences;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class ConfigurationCacheUnitTest {

    @Test
    public void saveConfiguration_savesConfigurationInSharedPrefs() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        Context context = ApplicationProvider.getApplicationContext();

        ConfigurationCache.saveConfiguration(context, configuration, "cacheKey", 123);

        assertEquals(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN, getSharedPreferences(context).getString("cacheKey", ""));
        assertEquals(123L, getSharedPreferences(context).getLong("cacheKey_timestamp", 0));
    }

    @Test
    public void getCacheConfiguration_returnsConfigurationFromSharedPrefs() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        Context context = ApplicationProvider.getApplicationContext();

        ConfigurationCache.saveConfiguration(context, configuration, "cacheKey", 0);

        assertEquals(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN, ConfigurationCache.getConfiguration(context, "cacheKey", TimeUnit.MINUTES.toMillis(5)-1));
    }

    @Test
    public void getCacheConfiguration_returnsNullIfCacheEntryExpires() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        Context context = ApplicationProvider.getApplicationContext();

        ConfigurationCache.saveConfiguration(context, configuration, "cacheKey", 0);

        assertNull(ConfigurationCache.getConfiguration(context, "cacheKey", TimeUnit.MINUTES.toMillis(5)));
    }
}