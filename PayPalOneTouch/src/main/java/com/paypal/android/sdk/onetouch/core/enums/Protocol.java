package com.paypal.android.sdk.onetouch.core.enums;

/**
 * Enum indicating the protocol version used to communicate to PayPal wallet or browser
 */
public enum Protocol {
    /**
     * Used for Hermes Checkout
     */
    v0("0.0"),

    /**
     * v1 wallet
     */
    v1("1.0"),

    /**
     * v2 wallet
     */
    v2("2.0"),

    /**
     * v3 browser switch consent
     */
    v3("3.0");

    private final String mVersion;

    Protocol(String version) {
        mVersion = version;
    }

    public String getVersion() {
        return mVersion;
    }

    public static Protocol getProtocol(String protocol) {
        switch (protocol) {
            case "0":
                return Protocol.v0;
            case "1":
                return Protocol.v1;
            case "2":
                return Protocol.v2;
            case "3":
                return Protocol.v3;
            default:
                throw new IllegalArgumentException("invalid protocol");
        }
    }
}