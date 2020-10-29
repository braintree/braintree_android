package com.braintreepayments.testutils;

import android.util.Base64;

import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.api.internal.StreamHelper;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
