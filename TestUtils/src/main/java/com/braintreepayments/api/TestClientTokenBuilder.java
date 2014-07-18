package com.braintreepayments.api;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class TestClientTokenBuilder {

    private static final int MERCHANT_WITHOUT_PAYPAL = 0;
    private static final int MERCHANT_WITH_PAYPAL = 1;
    private static final int MERCHANT_WITH_FAKE_PAYPAL = 2;
    private static final int MERCHANT_WITH_CVV_VERIFICATION = 3;
    private static final int MERCHANT_WITH_POSTAL_CODE_VERIFICATION = 4;
    private static final int MERCHANT_WITH_CVV_AND_POSTAL_CODE_VERIFICATION = 5;

    private boolean mWithCustomer = true;
    private int mMerchantType = MERCHANT_WITHOUT_PAYPAL;
    private boolean mAnalytics = false;
    private ArrayList<String> mChallenges = new ArrayList<String>() {{ add("cvv"); add("postal_code"); }};
    private boolean mRevoked = false;

    public TestClientTokenBuilder withoutCustomer() {
        mWithCustomer = false;
        return this;
    }

    public TestClientTokenBuilder withPayPal() {
        mMerchantType = MERCHANT_WITH_PAYPAL;
        return this;
    }

    public TestClientTokenBuilder withFakePayPal() {
        mMerchantType = MERCHANT_WITH_FAKE_PAYPAL;
        return this;
    }

    public TestClientTokenBuilder withCvvVerification() {
        mMerchantType = MERCHANT_WITH_CVV_VERIFICATION;
        return this;
    }

    public TestClientTokenBuilder withPostalCodeVerification() {
        mMerchantType = MERCHANT_WITH_POSTAL_CODE_VERIFICATION;
        return this;
    }

    public TestClientTokenBuilder withCvvAndPostalCodeVerification() {
        mMerchantType = MERCHANT_WITH_CVV_AND_POSTAL_CODE_VERIFICATION;
        return this;
    }

    public TestClientTokenBuilder withAnalytics() {
        mAnalytics = true;
        return this;
    }

    public TestClientTokenBuilder withoutCvvChallenge() {
        mChallenges.remove("cvv");
        return this;
    }

    public TestClientTokenBuilder withoutPostalCodeChallenge() {
        mChallenges.remove("postal_code");
        return this;
    }

    public TestClientTokenBuilder withRevokedClientToken() {
        mRevoked = true;
        return this;
    }

    public String build() {
        switch (mMerchantType) {
            case MERCHANT_WITHOUT_PAYPAL:
                return getClientTokenFromGateway("integration2_merchant_id", "integration2_public_key");
            case MERCHANT_WITH_PAYPAL:
                return getClientTokenFromGateway("altpay_merchant", "altpay_merchant_public_key");
            case MERCHANT_WITH_FAKE_PAYPAL:
                return getClientTokenFromGateway("integration_merchant_id", "integration_public_key");
            case MERCHANT_WITH_CVV_VERIFICATION:
                return getClientTokenFromGateway("client_api_cvv_verification_merchant_id", "client_api_cvv_verification_public_key");
            case MERCHANT_WITH_POSTAL_CODE_VERIFICATION:
                return getClientTokenFromGateway("client_api_postal_code_verification_merchant_id", "client_api_postal_code_verification_public_key");
            case MERCHANT_WITH_CVV_AND_POSTAL_CODE_VERIFICATION:
                return getClientTokenFromGateway("client_api_cvv_and_postal_code_verification_merchant_id", "client_api_cvv_and_postal_code_verification_public_key");
            default:
                throw new RuntimeException("Invalid merchant type specified!");
        }
    }

    private String getClientTokenFromGateway(String merchantId, String merchantPublicKey) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(EnvironmentHelper.getGatewayPath() + "/merchants/" + merchantId
                    + "/client_api/testing/client_token");

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            JSONObject json = new JSONObject();
            json.put("public_key", merchantPublicKey);
            json.put("customer", "true");
            json.put("token_version", "2");

            JSONObject overrides = new JSONObject();
            JSONArray challenges = new JSONArray();
            for (String challenge : mChallenges) {
                challenges.put(challenge);
            }
            overrides.put("challenges", challenges);

            if (mRevoked) {
                overrides.put("options", new JSONObject().put("revoked", "true"));
            }

            if (!mWithCustomer) {
                json.put("no_customer", "true");
            }
            if (mAnalytics) {
                json.put("analytics", "true");
            }

            json.put("overrides", overrides);

            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.writeBytes(json.toString());
            out.flush();
            out.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            String encodedToken = new JSONObject(response.toString()).getString("clientToken");
            String clientToken = new String(Base64.decode(encodedToken, Base64.DEFAULT));
            clientToken = clientToken.replaceAll("localhost", EnvironmentHelper.getGatewayIp());
            return Base64.encodeToString(clientToken.getBytes(), Base64.NO_WRAP);
        } catch (MalformedURLException e) {
            throw new RuntimeException("The url to your Gateway was invalid");
        } catch (IOException e) {
            throw new RuntimeException("There was an error connecting to your Gateway: " + e.getMessage());
        } catch (JSONException e) {
            throw new RuntimeException("There was an error building your json request: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
