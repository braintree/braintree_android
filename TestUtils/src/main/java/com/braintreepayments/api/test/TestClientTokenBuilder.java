package com.braintreepayments.api.test;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class TestClientTokenBuilder {

    private static String mCachedClientToken;
    private static Date mTimestamp;

    private boolean mWithCustomerId;
    private boolean mWithCvvVerification;
    private boolean mWithPostalCodeVerification;

    public TestClientTokenBuilder withCustomerId() {
        mWithCustomerId = true;
        return this;
    }

    public TestClientTokenBuilder withCvvVerification() {
        mWithCvvVerification = true;
        return this;
    }

    public TestClientTokenBuilder withPostalCodeVerification() {
        mWithPostalCodeVerification = true;
        return this;
    }

    public String build() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -30);

        if (mTimestamp == null || mTimestamp.before(calendar.getTime())) {
            mCachedClientToken = fetchClientToken();
            mTimestamp = new Date();
        }

        return mCachedClientToken;
    }

    private String fetchClientToken() {
        try {

            URL url;
            if (mWithCustomerId) {
                url = new URL("https://braintree-sample-merchant.herokuapp.com/client_token?customer_id=" + UUID.randomUUID().toString());
            } else {
                url = new URL("https://braintree-sample-merchant.herokuapp.com/client_token");
            }

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoInput(true);

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder responseBody = new StringBuilder();
            while ((line = in.readLine()) != null) {
                responseBody.append(line);
            }
            in.close();

            connection.disconnect();

            JSONObject json = new JSONObject(responseBody.toString());
            return json.getString("client_token");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid url");
        } catch (IOException e) {
            throw new RuntimeException("Error connecting to sample merchant server: " + e.getMessage());
        } catch (JSONException e) {
            throw new RuntimeException("Invalid json: " + e.getMessage());
        }
    }
}
