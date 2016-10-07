package com.braintreepayments.api.models;

import org.json.JSONObject;

public class VisaCheckoutConfiguration {

    public static boolean isVisaPackageAvailable() {
        try {
            Class.forName("com.visa.checkout.VisaPaymentInfo");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean mIsEnabled = false;
    private String mApiKey = "";
    private String mExternalClientId = "";

    static VisaCheckoutConfiguration fromJson(JSONObject json) {
        VisaCheckoutConfiguration visaCheckoutConfiguration = new VisaCheckoutConfiguration();

        if (json == null) {
            return visaCheckoutConfiguration;
        }

        visaCheckoutConfiguration.mIsEnabled = isVisaPackageAvailable();
        visaCheckoutConfiguration.mApiKey = json.optString("apikey");
        visaCheckoutConfiguration.mExternalClientId = json.optString("externalClientId");


        return visaCheckoutConfiguration;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    public String getExternalClientId() {
        return mExternalClientId;
    }

    public String getApiKey() {
        return mApiKey;
    }
}
