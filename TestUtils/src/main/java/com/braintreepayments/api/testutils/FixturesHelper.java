package com.braintreepayments.api.testutils;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FixturesHelper {

    public static String base64Encode(String value) {
        try {
            return Base64.encodeToString(value.getBytes(), Base64.NO_WRAP);
        } catch (RuntimeException | Error e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream streamFromString(String string) {
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }
}
