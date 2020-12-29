package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.internal.HttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;

import org.json.JSONException;

// TODO: Re-name to ConfigurationLoader
public class ConfigurationManager {

    private final BraintreeHttpClient httpClient;

    ConfigurationManager(BraintreeHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    void loadConfiguration(final Context context, final Authorization authorization, final ConfigurationCallback callback) {
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
                public void success(String responseBody) {
                    try {
                        Configuration configuration = Configuration.fromJson(responseBody);
                        saveConfigurationToCache(context, configuration, authorization, configUrl);
                        callback.onResult(configuration, null);
                    } catch (JSONException jsonException) {
                        callback.onResult(null, jsonException);
                    }
                }

                @Override
                public void failure(Exception httpException) {
                    String errorMessageFormat = "Request for configuration has failed: %s";
                    String errorMessage = String.format(errorMessageFormat, httpException.getMessage());

                    ConfigurationException configurationException = new ConfigurationException(errorMessage, httpException);
                    callback.onResult(null, configurationException);
                }
            });
        }
    }

    private static void saveConfigurationToCache(Context context, Configuration configuration, Authorization authorization, String configUrl) {
        String cacheKey = createCacheKey(authorization, configUrl);
        ConfigurationCache.saveConfiguration(context, configuration, cacheKey);
    }

    private static Configuration getCachedConfiguration(Context context, Authorization authorization, String configUrl) {
        String cacheKey = createCacheKey(authorization, configUrl);
        String cachedConfigResponse = ConfigurationCache.getConfiguration(context, cacheKey);
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
