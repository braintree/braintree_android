package com.paypal.android.sdk.onetouch.core.sdk;

/**
 * Scopes that a user may be asked to consented to.  This class is for internal use only.
 */
public enum PayPalScope {

    /**
     * Allows payments to be made in the future using PayPal
     */
    FUTURE_PAYMENTS("https://uri.paypal.com/services/payments/futurepayments"),

    /**
     * Account profile
     */
    PROFILE("profile"),

    /**
     * PayPal attributes - personal and account information
     */
    PAYPAL_ATTRIBUTES("https://uri.paypal.com/services/paypalattributes"),

    /**
     * openid
     */
    OPENID("openid"),

    /**
     * email
     */
    EMAIL("email"),

    /**
     * address
     */
    ADDRESS("address"),

    /**
     * phone
     */
    PHONE("phone");

    private String mScopeUri;

    PayPalScope(String scopeUri) {
        mScopeUri = scopeUri;
    }

    public String getScopeUri() {
        return mScopeUri;
    }
}
