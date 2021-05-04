package com.braintreepayments.api;

import com.braintreepayments.api.models.BuildConfig;

class TokenizationKey extends Authorization {

    static final String MATCHER = "^[a-zA-Z0-9]+_[a-zA-Z0-9]+_[a-zA-Z0-9_]+$";

    private final String environment;
    private final String merchantId;
    private final String url;

    TokenizationKey(String tokenizationKey) throws InvalidArgumentException {
        super(tokenizationKey);

        String[] tokenizationKeyParts = tokenizationKey.split("_", 3);
        environment = tokenizationKeyParts[0];
        merchantId = tokenizationKeyParts[2];
        url = BraintreeEnvironment.getUrl(environment) + "merchants/" +
                merchantId + "/client_api/";
    }

    String getEnvironment() {
        return environment;
    }

    String getMerchantId() {
        return merchantId;
    }

    String getUrl() {
        return url;
    }

    @Override
    String getConfigUrl() {
        return url + "v1/configuration";
    }

    @Override
    String getBearer() {
        return toString();
    }

    private enum BraintreeEnvironment {

        DEVELOPMENT("development", BuildConfig.DEVELOPMENT_URL),
        SANDBOX("sandbox", "https://api.sandbox.braintreegateway.com/"),
        PRODUCTION("production", "https://api.braintreegateway.com/");

        private final String mEnvironment;
        private final String mUrl;

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
            throw new InvalidArgumentException("Tokenization Key contained invalid environment");
        }
    }
}
