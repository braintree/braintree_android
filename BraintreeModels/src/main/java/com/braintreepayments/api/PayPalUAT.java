package com.braintreepayments.api;

import android.os.Parcel;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class PayPalUAT extends Authorization {

    static final String MATCHER = "^[a-zA-Z0-9]+\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9_-]+$";

    private static final String EXTERNAL_ID_STRING = "external_id";

    private String mConfigUrl;
    private String mPayPalUrl;
    private String mBraintreeMerchantID;
    private String mToken;
    private Environment mEnvironment;

    enum Environment {
        STAGING,
        SANDBOX,
        PRODUCTION
    }

    /**
     * Create a new {@link PayPalUAT} instance from a PayPal universal access token
     *
     * @param uatString A PayPal UAT from PayPal's oauth service
     */
    PayPalUAT(String uatString) throws InvalidArgumentException {
        super(uatString);

        mToken = uatString;

        try {
            String decodedUATString = decodeUATString(uatString);
            JSONObject jsonObject = new JSONObject(decodedUATString);

            JSONArray externalIDs = jsonObject.getJSONArray(EXTERNAL_ID_STRING);

            for (int i = 0; i < externalIDs.length(); i++) {
                if (externalIDs.getString(i).startsWith("Braintree:")) {
                    mBraintreeMerchantID = externalIDs.getString(i).split(":")[1];
                    break;
                }
            }

            if (TextUtils.isEmpty(mBraintreeMerchantID)) {
                throw new IllegalArgumentException("Missing Braintree merchant account ID.");
            }

            if (jsonObject.has("iss")) {
                mPayPalUrl = jsonObject.getString("iss");
                mEnvironment = determineIssuerEnv();
                mConfigUrl = generateConfigUrl();
            } else {
                throw new IllegalArgumentException("Does not contain issuer, or \"iss\" key.");
            }
        } catch (NullPointerException | IllegalArgumentException | JSONException e) {
            throw new InvalidArgumentException("PayPal UAT invalid: " + e.getMessage());
        }
    }

    private String decodeUATString(String uat) {
        String[] uatComponents = uat.split("[.]");
        return new String(Base64.decode(uatComponents[1], Base64.DEFAULT));
    }

    private String generateConfigUrl() {
        String baseBraintreeURL;
        if (mEnvironment == Environment.STAGING || mEnvironment == Environment.SANDBOX) {
            baseBraintreeURL = "https://api.sandbox.braintreegateway.com:443/merchants/";
        } else {
            baseBraintreeURL = "https://api.braintreegateway.com:443/merchants/";
        }

        return baseBraintreeURL + mBraintreeMerchantID + "/client_api/v1/configuration";
    }

    private Environment determineIssuerEnv() throws IllegalArgumentException {
        switch (mPayPalUrl) {
            case "https://api.paypal.com":
                return Environment.PRODUCTION;
            case "https://api.sandbox.paypal.com":
                return Environment.SANDBOX;
            case "https://api.msmaster.qa.paypal.com":
                return Environment.STAGING;
            default:
                throw new IllegalArgumentException("PayPal issuer URL missing or unknown: " + mPayPalUrl);
        }
    }

    @Override
    String getConfigUrl() {
        return mConfigUrl;
    }

    @Override
    String getBearer() {
        return mToken;
    }

    /**
     * @return The base PayPal URL
     */
    String getPayPalURL() {
        return mPayPalUrl;
    }

    /**
     * @return The environment context of the provided PayPal UAT
     */
    Environment getEnvironment() {
        return mEnvironment;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mConfigUrl);
        dest.writeString(mPayPalUrl);
        dest.writeString(mToken);
        dest.writeString(mBraintreeMerchantID);
    }

    protected PayPalUAT(Parcel in) {
        super(in);
        mConfigUrl = in.readString();
        mPayPalUrl = in.readString();
        mToken = in.readString();
        mBraintreeMerchantID = in.readString();
    }

    public static final Creator<PayPalUAT> CREATOR = new Creator<PayPalUAT>() {
        public PayPalUAT createFromParcel(Parcel source) {
            return new PayPalUAT(source);
        }

        public PayPalUAT[] newArray(int size) {
            return new PayPalUAT[size];
        }
    };
}
