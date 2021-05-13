package com.braintreepayments.api;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains the remote Samsung Pay configuration for the Braintree SDK.
 */
class SamsungPayConfiguration {

    private static final String DISPLAY_NAME_KEY = "displayName";
    private static final String SERVICE_ID_KEY = "serviceId";
    private static final String SUPPORTED_CARD_BRANDS_KEY = "supportedCardBrands";
    private static final String SAMSUNG_AUTHORIZATION_KEY = "samsungAuthorization";
    private static final String ENVIRONMENT = "environment";

    private final List<String> supportedCardBrands = new ArrayList<>();
    private String merchantDisplayName;
    private String serviceId;
    private String samsungAuthorization;
    private String environment;

    static SamsungPayConfiguration fromJson(JSONObject json) {
        SamsungPayConfiguration configuration = new SamsungPayConfiguration();

        if (json == null) {
            json = new JSONObject();
        }

        configuration.merchantDisplayName = Json.optString(json, DISPLAY_NAME_KEY, "");
        configuration.serviceId = Json.optString(json, SERVICE_ID_KEY, "");

        try {
            JSONArray supportedCardBrands = json.getJSONArray(SUPPORTED_CARD_BRANDS_KEY);
            for (int i = 0; i < supportedCardBrands.length(); i++) {
                configuration.supportedCardBrands.add(supportedCardBrands.getString(i));
            }
        } catch (JSONException ignored) {}

        configuration.samsungAuthorization = Json.optString(json, SAMSUNG_AUTHORIZATION_KEY, "");
        configuration.environment = Json.optString(json, ENVIRONMENT, "");

        return configuration;
    }

    /**
     * @return {@code true} if Samsung Pay is enabled, {@code false} otherwise.
     */
    boolean isEnabled() {
        return !TextUtils.isEmpty(samsungAuthorization);
    }

    /**
     * @return the merchant display name for Samsung Pay.
     */
    @NonNull
    String getMerchantDisplayName() {
        return merchantDisplayName;
    }

    /**
     * @return the service id associated with the merchant.
     */
    @NonNull
    String getServiceId() {
        return serviceId;
    }

    /**
     * @return a list of card brands supported by Samsung Pay.
     */
    @NonNull
    List<String> getSupportedCardBrands() {
        return Collections.unmodifiableList(supportedCardBrands);
    }

    /**
     * @return the authorization to use with Samsung Pay.
     */
    @NonNull
    String getSamsungAuthorization() {
        return samsungAuthorization;
    }

    /**
     * @return the Braintree environment Samsung Pay should interact with.
     */
    String getEnvironment() {
        return environment;
    }
}
