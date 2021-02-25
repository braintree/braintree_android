package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

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

    private static final String SAMSUNG_PAY_CLASSNAME = "com.braintreepayments.api.SamsungPay";
    private static final String DISPLAY_NAME_KEY = "displayName";
    private static final String SERVICE_ID_KEY = "serviceId";
    private static final String SUPPORTED_CARD_BRANDS_KEY = "supportedCardBrands";
    private static final String SAMSUNG_AUTHORIZATION_KEY = "samsungAuthorization";
    private static final String ENVIRONMENT = "environment";

    private final List<String> mSupportedCardBrands = new ArrayList<>();
    private String mMerchantDisplayName;
    private String mServiceId;
    private String mSamsungAuthorization;
    private String mEnvironment;

    private final ClassHelper classHelper;

    SamsungPayConfiguration() {
        this(new ClassHelper());
    }

    @VisibleForTesting
    SamsungPayConfiguration(ClassHelper classHelper) {
        this.classHelper = classHelper;
    }

    static SamsungPayConfiguration fromJson(JSONObject json) {
        SamsungPayConfiguration configuration = new SamsungPayConfiguration();

        if (json == null) {
            json = new JSONObject();
        }

        configuration.mMerchantDisplayName = Json.optString(json, DISPLAY_NAME_KEY, "");
        configuration.mServiceId = Json.optString(json, SERVICE_ID_KEY, "");

        try {
            JSONArray supportedCardBrands = json.getJSONArray(SUPPORTED_CARD_BRANDS_KEY);
            for (int i = 0; i < supportedCardBrands.length(); i++) {
                configuration.mSupportedCardBrands.add(supportedCardBrands.getString(i));
            }
        } catch (JSONException ignored) {}

        configuration.mSamsungAuthorization = Json.optString(json, SAMSUNG_AUTHORIZATION_KEY, "");
        configuration.mEnvironment = Json.optString(json, ENVIRONMENT, "");

        return configuration;
    }

    /**
     * @return {@code true} if Samsung Pay is enabled, {@code false} otherwise.
     */
    boolean isEnabled() {
        return !"".equals(mSamsungAuthorization) &&
                classHelper.isClassAvailable(SAMSUNG_PAY_CLASSNAME);
    }

    /**
     * @return the merchant display name for Samsung Pay.
     */
    @NonNull
    String getMerchantDisplayName() {
        return mMerchantDisplayName;
    }

    /**
     * @return the service id associated with the merchant.
     */
    @NonNull
    String getServiceId() {
        return mServiceId;
    }

    /**
     * @return a list of card brands supported by Samsung Pay.
     */
    @NonNull
    List<String> getSupportedCardBrands() {
        return Collections.unmodifiableList(mSupportedCardBrands);
    }

    /**
     * @return the authorization to use with Samsung Pay.
     */
    @NonNull
    String getSamsungAuthorization() {
        return mSamsungAuthorization;
    }

    /**
     * @return the Braintree environment Samsung Pay should interact with.
     */
    String getEnvironment() {
        return mEnvironment;
    }
}
