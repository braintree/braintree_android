package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.models.BuildConfig;

class TokenizationKey extends Authorization implements Parcelable {

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
            throw new InvalidArgumentException("Tokenization Key contained invalid environment");
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(environment);
        dest.writeString(merchantId);
        dest.writeString(url);
    }

    protected TokenizationKey(Parcel in) {
        super(in);
        environment = in.readString();
        merchantId = in.readString();
        url = in.readString();
    }

    public static final Creator<TokenizationKey> CREATOR = new Creator<TokenizationKey>() {
        public TokenizationKey createFromParcel(Parcel source) {
            return new TokenizationKey(source);
        }

        public TokenizationKey[] newArray(int size) {
            return new TokenizationKey[size];
        }
    };
}
