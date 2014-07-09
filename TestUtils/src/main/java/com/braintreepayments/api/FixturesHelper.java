package com.braintreepayments.api;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class FixturesHelper {
    public static final String FIXTURES_PATH = "fixtures/";

    public static String stringFromFixture(Context context, String fixtureFilename) {
        try {
            InputStream is = context.getResources().getAssets()
                    .open(FIXTURES_PATH + fixtureFilename);

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            try {
                String line;

                StringBuffer data = new StringBuffer();
                while ((line = reader.readLine()) != null) {
                    data.append(line);
                }

                return data.toString();
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
