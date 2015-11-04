package com.paypal.android.sdk.onetouch.core.base;

public interface Crypto {
    String encryptIt(String value);

    String decryptIt(String value);
}
