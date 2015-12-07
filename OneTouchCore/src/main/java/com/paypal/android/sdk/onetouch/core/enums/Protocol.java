package com.paypal.android.sdk.onetouch.core.enums;

/**
 * Enum indicating the protocol version used to communicate to PayPal wallet or browser
 */
public enum Protocol {
    /**
     * Not really defined?  Used for Hermes Checkout
     */
    v0,

    /**
     * v1 wallet
     */
    v1,

    /**
     * v2 wallet
     */
    v2,

    /**
     * v3 browser switch consent
     */
    v3
}