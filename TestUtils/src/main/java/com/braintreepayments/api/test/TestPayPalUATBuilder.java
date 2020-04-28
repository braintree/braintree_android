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

public class TestPayPalUATBuilder {

    private static String mCachedPayPalUAT;
    private static Date mTimestamp;

    public String build() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -30);

        if (mTimestamp == null || mTimestamp.before(calendar.getTime())) {
            mCachedPayPalUAT = fetchUAT();
            mTimestamp = new Date();
        }

        return mCachedPayPalUAT;
    }

    public String fetchUAT() {
        try {

            URL url = new URL("https://ppcp-sample-merchant-sand.herokuapp.com/uat?countryCode=US");

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
            return json.getString("universal_access_token");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid url");
        } catch (IOException e) {
            throw new RuntimeException("Error connecting to PPCP sample merchant server: " + e.getMessage());
        } catch (JSONException e) {
            throw new RuntimeException("Invalid json: " + e.getMessage());
        }
    }
}
