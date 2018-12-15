package com.braintreepayments.api.test;

import com.braintreepayments.api.internal.StreamHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static androidx.test.InstrumentationRegistry.getTargetContext;

public class FixturesHelper {

    private static final String FIXTURES_PATH = "fixtures/";
    private static final String LOCAL_UNIT_TEST_FIXTURES_PATH = "src/test/assets/" + FIXTURES_PATH;

    public static String stringFromFixture(String filename) {
        try {
            try {
                return stringFromAndroidFixture(filename);
            } catch (RuntimeException | Error e) {
                return stringFromUnitTestFixture(filename);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String stringFromAndroidFixture(String filename) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = getTargetContext().getResources().getAssets().open(FIXTURES_PATH + filename);
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
