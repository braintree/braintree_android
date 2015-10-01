package com.braintreepayments.api.models;

import com.braintreepayments.api.annotations.Beta;
import com.google.gson.annotations.SerializedName;

/**
 * Contains the remote Coinbase configuration for the Braintree SDK
 */
@Beta
public class CoinbaseConfiguration {

    @SerializedName("clientId") private String mClientId;
    @SerializedName("merchantAccount") private String mMerchantAccount;
    @SerializedName("scopes") private String mScopes;
    @SerializedName("environment") private String mEnvironment;

    /**
     * @return the Coinbase client id.
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * @return the Coinbase merchant account.
     */
    public String getMerchantAccount() {
        return mMerchantAccount;
    }

    /**
     * @return the scopes requested for the current Coinbase app.
     */
    public String getScopes() {
        return mScopes;
    }

    /**
     * @return the Coinbase environment. E.g. 'shared_sandbox'
     */
    public String getEnvironment() {
        return mEnvironment;
    }

    /**
     * @return the Coinbase baseURL for the environment
     */
    public String getBaseURLForEnvironment() {
        return getEnvironment().equalsIgnoreCase("shared_sandbox") ? "https://sandbox.coinbase.com/" : "https://www.coinbase.com/";
    }
}
