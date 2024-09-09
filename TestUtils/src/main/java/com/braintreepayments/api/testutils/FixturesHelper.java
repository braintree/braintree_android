package com.braintreepayments.api.testutils;

import android.util.Base64;

public class FixturesHelper {

    public static String base64Encode(String value) {
        try {
            return Base64.encodeToString(value.getBytes(), Base64.NO_WRAP);
        } catch (RuntimeException | Error e) {
            throw new RuntimeException(e);
        }
    }
}
