package com.paypal.android.networking;

import java.util.HashMap;
import java.util.Map;

public class PayPalEnvironment {
    private String mServerName;
    private String mBaseUrl;
    private Map<String, String> mEndpoints = new HashMap<>();

    public PayPalEnvironment(String environmentName, String baseUrl) {
        this.mServerName = environmentName;
        this.mBaseUrl = baseUrl;
    }

    public String getServerName() {
        return mServerName;
    }

    public String getBaseUrl() {
        return mBaseUrl;
    }

    public Map<String, String> getEndpoints() {
        return mEndpoints;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + mServerName + ",mEndpoints=" + mEndpoints + ")";
    }

}
