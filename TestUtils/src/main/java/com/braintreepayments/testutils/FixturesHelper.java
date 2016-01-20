package com.braintreepayments.testutils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import static android.support.test.InstrumentationRegistry.getTargetContext;

public class FixturesHelper {

    private static final String FIXTURES_PATH = "fixtures/";
    private static final String LOCAL_UNIT_TEST_FIXTURES_PATH = "src/androidTest/assets/" + FIXTURES_PATH;

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
            return getStringFromInputStream(inputStream);
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
            return getStringFromInputStream(inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private static String getStringFromInputStream(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName("UTF-8")));
        try {
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                data.append(line);
            }

            return data.toString();
        } finally {
            reader.close();
        }
    }
}
