package com.braintreepayments.api;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class FixturesHelper {

    public static String base64Encode(String value) {
        try {
            return Base64.encodeToString(value.getBytes(), Base64.NO_WRAP);
        } catch (RuntimeException | Error e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream streamFromString(String string) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(string.getBytes("UTF-8"));
    }
}
