package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.InvalidArgumentException;

public class ClientKey extends Authorization implements Parcelable {

    protected static String MATCHER = "^[a-zA-Z0-9_]+_[a-zA-Z0-9]+_[a-zA-Z0-9_]+$";

    private final String mEnvironment;
    private final String mMerchantId;
    private final String mUrl;

    ClientKey(String clientKeyString) throws InvalidArgumentException {
        super(clientKeyString);

        String[] clientKeyParts = clientKeyString.split("_", 3);
        mEnvironment = clientKeyParts[0];
        mMerchantId = clientKeyParts[2];
        mUrl = BraintreeEnvironment.getUrl(mEnvironment) + "merchants/" +
                mMerchantId + "/client_api/";
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

    @Override
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mEnvironment);
        dest.writeString(mMerchantId);
        dest.writeString(mUrl);
    }

    protected ClientKey(Parcel in) {
        super(in);
        mEnvironment = in.readString();
        mMerchantId = in.readString();
        mUrl = in.readString();
    }

    public static final Creator<ClientKey> CREATOR = new Creator<ClientKey>() {
        public ClientKey createFromParcel(Parcel source) {
            return new ClientKey(source);
        }

        public ClientKey[] newArray(int size) {
            return new ClientKey[size];
        }
    };
}
