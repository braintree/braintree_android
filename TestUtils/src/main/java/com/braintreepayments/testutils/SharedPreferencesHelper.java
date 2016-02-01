package com.braintreepayments.testutils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import static android.support.test.InstrumentationRegistry.getTargetContext;

/**
 * Provides managed access to shared preferences used by Braintree, et al.
 */
public class SharedPreferencesHelper {

    /**
     * @return Shared Preferences used in Braintree
     */
    public static SharedPreferences getSharedPreferences() {
        return getTargetContext().getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE);
    }

    /**
     * Writes mock configuration data to Configuration cache
     *
     * @param configUrl
     * @param configurationString
     * @param timestamp
     */
    public static void writeMockConfiguration(String configUrl, String configurationString, long timestamp) {
        configUrl = Uri.parse(configUrl)
                .buildUpon()
                .appendQueryParameter("configVersion", "3")
                .build().toString();

        String key = Base64.encodeToString(configUrl.getBytes(), 0);
        getSharedPreferences().edit().putString(key, configurationString).commit();
        getSharedPreferences().edit().putLong(key + "_timestamp", timestamp).commit();
    }

    public static void writeMockConfiguration(String configUrl, String configurationString) {
        writeMockConfiguration(configUrl, configurationString, System.currentTimeMillis());
    }
}
