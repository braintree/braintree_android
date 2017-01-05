package com.braintreepayments.api.models;

import com.braintreepayments.api.Json;

import org.json.JSONObject;

/**
 * Contains the remote Visa Checkout configuration for the Braintree SDK.
 */
public class VisaCheckoutConfiguration {

    private boolean mIsEnabled;
    private String mApiKey;
    private String mExternalClientId;

    /**
     * Determines if the Visa Checkout SDK is available.
     * @return true when the class can be found, false otherwise.
     */
    public static boolean isVisaCheckoutSDKAvailable() {
        try {
            Class.forName("com.visa.checkout.VisaPaymentInfo");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static VisaCheckoutConfiguration fromJson(JSONObject json) {
        VisaCheckoutConfiguration visaCheckoutConfiguration = new VisaCheckoutConfiguration();

        if (json == null) {
            json = new JSONObject();
        } else {
            visaCheckoutConfiguration.mIsEnabled = isVisaCheckoutSDKAvailable();
        }

        visaCheckoutConfiguration.mApiKey = Json.optString(json, "apikey", "");
        visaCheckoutConfiguration.mExternalClientId = Json.optString(json, "externalClientId", "");

        return visaCheckoutConfiguration;
    }

    /**
     * Determines if the Visa Checkout flow is available to be used. This can be used to determine
     * if UI components should be shown or hidden.
     *
     * @return boolean if Visa Checkout SDK is available, and configuration is enabled.
     */
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * @return The Visa Checkout External Client Id associated with this merchant's Visa Checkout configuration.
     */
    public String getExternalClientId() {
        return mExternalClientId;
    }

    /**
     * @return The Visa Checkout API Key associated with this merchant's Visa Checkout configuration.
     */
    public String getApiKey() {
        return mApiKey;
    }
}
