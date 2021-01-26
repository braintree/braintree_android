package com.braintreepayments.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class StreamHelper {

    public static String getString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
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
