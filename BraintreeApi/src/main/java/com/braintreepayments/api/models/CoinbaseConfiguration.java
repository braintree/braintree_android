package com.braintreepayments.api.models;

/**
 * Contains the remote Coinbase configuration for the Braintree SDK
 */
public class CoinbaseConfiguration {

    private String clientId;
    private String merchantAccount;
    private String scopes;
    private String environment;

    /**
     * @return the Coinbase client id.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @return the Coinbase merchant account.
     */
    public String getMerchantAccount() {
        return merchantAccount;
    }

    /**
     * @return the scopes requested for the current Coinbase app.
     */
    public String getScopes() {
        return scopes;
    }

    /**
     * @return the Coinbase environment. E.g. 'shared_sandbox'
     */
    public String getEnvironment() { return environment; }

    /**
     * @return the Coinbase baseURL for the environment
     */
    public String getBaseURLForEnvironment() { return getEnvironment().equalsIgnoreCase("shared_sandbox") ? "https://sandbox.coinbase.com/" : "https://www.coinbase.com/"; }

}
