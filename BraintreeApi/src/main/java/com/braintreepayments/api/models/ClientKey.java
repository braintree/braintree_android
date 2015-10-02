package com.braintreepayments.api.models;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.InvalidArgumentException;

public class ClientKey {

    private String mClientKey;
    private String mEnvironment;
    private String mMerchantId;
    private String mUrl;

    public static ClientKey fromString(String clientKeyString) throws InvalidArgumentException {
        if (!clientKeyString.matches("^[a-zA-Z0-9_]+_[a-zA-Z0-9]+_[a-zA-Z0-9_]+$")) {
            throw new InvalidArgumentException("Client Key is not a valid format");
        }

        String[] clientKeyParts = clientKeyString.split("_", 3);
        ClientKey clientKey = new ClientKey();
        clientKey.mClientKey = clientKeyString;
        clientKey.mEnvironment = clientKeyParts[0];
        clientKey.mMerchantId = clientKeyParts[2];
        clientKey.mUrl = BraintreeEnvironment.getUrl(clientKey.mEnvironment) + "merchants/" +
                clientKey.mMerchantId + "/client_api/";

        return clientKey;
    }

    public String clientKeyString() {
        return mClientKey;
    }

    public String getEnvironment() {
        return mEnvironment;
    }

    public String getMerchantId() {
        return mMerchantId;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getConfigUrl() {
        return mUrl + "v1/configuration";
    }

    private enum BraintreeEnvironment {

        DEVELOPMENT("development", BuildConfig.DEVELOPMENT_URL),
        SANDBOX("sandbox", "https://api.sandbox.braintreegateway.com/"),
        PRODUCTION("production", "https://api.braintreegateway.com/");

        private String mEnvironment;
        private String mUrl;

        BraintreeEnvironment(String environment, String url) {
            mEnvironment = environment;
            mUrl = url;
        }

        static String getUrl(String environment) throws InvalidArgumentException {
            for (BraintreeEnvironment braintreeEnvironment : BraintreeEnvironment.values()) {
                if (braintreeEnvironment.mEnvironment.equals(environment)) {
                    return braintreeEnvironment.mUrl;
                }
            }
            throw new InvalidArgumentException("Client key contained invalid environment");
        }
    }
}
