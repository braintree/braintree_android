package com.braintreepayments.testutils;

import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.api.internal.StreamHelper;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Base64;


public class FixturesHelper {

    private static final String FIXTURES_PATH = "fixtures/";
    private static final String LOCAL_UNIT_TEST_FIXTURES_PATH = "src/androidTest/assets/" + FIXTURES_PATH;

    public static String stringFromFixture(String filename) {
        try {
            try {
                return stringFromAndroidFixture(filename);
            } catch (RuntimeException | FileNotFoundException | Error e) {
                return stringFromUnitTestFixture(filename);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String base64EncodedClientTokenFromFixture(String filename) {
        try {
            try {
                return Base64.getEncoder().encodeToString(stringFromAndroidFixture(filename).getBytes());
            } catch (RuntimeException | FileNotFoundException | Error e) {
                return Base64.getEncoder().encodeToString(stringFromUnitTestFixture(filename).getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream streamFromString(String string) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(string.getBytes("UTF-8"));
    }

    private static String stringFromAndroidFixture(String filename) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = ApplicationProvider.getApplicationContext().getResources().getAssets().open(FIXTURES_PATH + filename);
            return StreamHelper.getString(inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private static String stringFromUnitTestFixture(String filename) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(LOCAL_UNIT_TEST_FIXTURES_PATH + filename);
            return StreamHelper.getString(inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
