package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Base64;

import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.TokenizationKey;

import org.json.JSONException;

import java.util.concurrent.TimeUnit;

/**
 * Manages on-disk {@link Configuration} cache and fetching configuration from the Gateway
 */
class ConfigurationManager {

    static final long TTL = TimeUnit.MINUTES.toMillis(5);
    @VisibleForTesting
    static boolean sFetchingConfiguration = false;

    private ConfigurationManager() {}

    static boolean isFetchingConfiguration() {
        return sFetchingConfiguration;
    }

    static void getConfiguration(final BraintreeFragment fragment, final @NonNull ConfigurationListener listener,
            final @NonNull BraintreeResponseListener<Exception> errorListener) {
        String authorization = "";
        if (fragment.getAuthorization() instanceof ClientToken) {
            try {
                authorization = ((ClientToken) fragment.getAuthorization()).getAuthorizationFingerprint()
                        .split("\\|")[1]
                        .replaceAll("created_at=.*?&", "");
            } catch (NullPointerException | ArrayIndexOutOfBoundsException ignored) {}
        } else if (fragment.getAuthorization() instanceof TokenizationKey) {
            authorization = fragment.getAuthorization().toString();
        }

        final String configUrl = Uri.parse(fragment.getAuthorization().getConfigUrl())
                .buildUpon()
                .appendQueryParameter("configVersion", "3")
                .build()
                .toString();

        Configuration cachedConfig = getCachedConfiguration(fragment.getApplicationContext(), configUrl + authorization);
        if (cachedConfig != null) {
            listener.onConfigurationFetched(cachedConfig);
        } else {
            sFetchingConfiguration = true;
            final String finalAuthorization = authorization;
            fragment.getHttpClient().get(configUrl, new HttpResponseCallback() {
                @Override
                public void success(String responseBody) {
                    try {
                        Configuration configuration = Configuration.fromJson(responseBody);
                        cacheConfiguration(fragment.getApplicationContext(), configUrl + finalAuthorization, configuration);

                        sFetchingConfiguration = false;
                        listener.onConfigurationFetched(configuration);
                    } catch (final JSONException e) {
                        sFetchingConfiguration = false;
                        errorListener.onResponse(e);
                    }
                }

                @Override
                public void failure(final Exception exception) {
                    sFetchingConfiguration = false;
                    errorListener.onResponse(exception);
                }
            });
        }
    }

    @Nullable
    private static Configuration getCachedConfiguration(Context context, String configUrl) {
        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(context);
        configUrl = Base64.encodeToString(configUrl.getBytes(), 0);

        String timestampKey = configUrl + "_timestamp";
        if ((System.currentTimeMillis() - prefs.getLong(timestampKey, 0)) > TTL) {
            return null;
        }

        try {
            return Configuration.fromJson(prefs.getString(configUrl, ""));
        } catch (JSONException e) {
            return null;
        }
    }

    private static void cacheConfiguration(Context context, String configUrl, Configuration configuration) {
        configUrl = Base64.encodeToString(configUrl.getBytes(), 0);

        String timestampKey = configUrl + "_timestamp";
        BraintreeSharedPreferences.getSharedPreferences(context).edit()
                .putString(configUrl, configuration.toJson())
                .putLong(timestampKey, System.currentTimeMillis())
                .apply();
    }
}
