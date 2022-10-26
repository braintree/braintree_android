package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

public class SharedPreferencesHelper {

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE);
    }

    public static SharedPreferences getSharedPreferences(Context context, String fileName) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    public static void overrideConfigurationCache(Context context, Authorization authorization, Configuration configuration) {
        final String configUrl = Uri.parse(authorization.getConfigUrl())
                .buildUpon()
                .appendQueryParameter("configVersion", "3")
                .build()
                .toString();

        String cacheKey = Base64.encodeToString(String.format("%s%s", configUrl, authorization.getBearer()).getBytes(), 0);
        String timestampKey = String.format("%s_timestamp", cacheKey);
        try {
            BraintreeSharedPreferences.getInstance().putStringAndLong(context, cacheKey, configuration.toJson(), timestampKey, System.currentTimeMillis());
        } catch (UnexpectedException ignored) {
        }
    }

    public static void clearConfigurationCacheOverride(Context context) {
        try {
            BraintreeSharedPreferences.getInstance().clearSharedPreferences(context);
        } catch (UnexpectedException ignored) {
        }
    }
}
