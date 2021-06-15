package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;

class ConfigurationLoader {

    private final BraintreeHTTPClient httpClient;
    private final ConfigurationCache configurationCache;

    ConfigurationLoader(BraintreeHTTPClient httpClient) {
        this(httpClient, ConfigurationCache.getInstance());
    }

    @VisibleForTesting
    ConfigurationLoader(BraintreeHTTPClient httpClient, ConfigurationCache configurationCache) {
        this.httpClient = httpClient;
        this.configurationCache = configurationCache;
    }

    void loadConfiguration(final Context context, final Authorization authorization, final ConfigurationCallback callback) {
        if (authorization instanceof InvalidAuthorization) {
            String message = ((InvalidAuthorization) authorization).getErrorMessage();
            callback.onResult(null, new BraintreeException(message));
            return;
        }

        final String configUrl = Uri.parse(authorization.getConfigUrl())
                .buildUpon()
                .appendQueryParameter("configVersion", "3")
                .build()
                .toString();

        Configuration cachedConfig = getCachedConfiguration(context, authorization, configUrl);
        if (cachedConfig != null) {
            callback.onResult(cachedConfig, null);
        } else {

            httpClient.get(configUrl, null, HttpClient.RETRY_MAX_3_TIMES, new HttpResponseCallback() {

                @Override
                public void onResult(String responseBody, Exception httpError) {
                    if (responseBody != null) {
                        try {
                            Configuration configuration = Configuration.fromJson(responseBody);
                            saveConfigurationToCache(context, configuration, authorization, configUrl);
                            callback.onResult(configuration, null);
                        } catch (JSONException jsonException) {
                            callback.onResult(null, jsonException);
                        }
                    } else {
                        String errorMessageFormat = "Request for configuration has failed: %s";
                        String errorMessage = String.format(errorMessageFormat, httpError.getMessage());

                        ConfigurationException configurationException = new ConfigurationException(errorMessage, httpError);
                        callback.onResult(null, configurationException);
                    }
                }
            });
        }
    }

    private void saveConfigurationToCache(Context context, Configuration configuration, Authorization authorization, String configUrl) {
        String cacheKey = createCacheKey(authorization, configUrl);
        configurationCache.saveConfiguration(context, configuration, cacheKey);
    }

    private Configuration getCachedConfiguration(Context context, Authorization authorization, String configUrl) {
        String cacheKey = createCacheKey(authorization, configUrl);
        String cachedConfigResponse = configurationCache.getConfiguration(context, cacheKey);
        try {
            return Configuration.fromJson(cachedConfigResponse);
        } catch (JSONException e) {
            return null;
        }
    }

    private static String createCacheKey(Authorization authorization, String configUrl) {
        return Base64.encodeToString(String.format("%s%s", configUrl, authorization.getBearer()).getBytes(), 0);
    }
}
