package com.braintreepayments.api.models;

/**
 * Contains the remote Coinbase configuration for the Braintree SDK
 */
public class CoinbaseConfiguration {

    private String clientId;
    private String merchantAccount;
    private String scopes;

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
}
