package com.braintreepayments.testutils;

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
    private static final int MERCHANT_WITH_THREE_D_SECURE_ENABLED = 6;
    private static final int MERCHANT_WITH_COINBASE_ENABLED = 7;

    private boolean mWithCustomer = true;
    private int mMerchantType = MERCHANT_WITHOUT_PAYPAL;
    private String mMerchantAccount = null;
    private boolean mAnalytics = false;
    private ArrayList<String> mChallenges = new ArrayList<String>() {{ add("cvv"); add("postal_code"); }};
    private boolean mTouchEnabled = false;
    private String mVenmoEnvironment = null;

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

    public TestClientTokenBuilder withCoinbase() {
        mMerchantType = MERCHANT_WITH_COINBASE_ENABLED;
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

    public TestClientTokenBuilder withThreeDSecure() {
        mMerchantType = MERCHANT_WITH_THREE_D_SECURE_ENABLED;
        mMerchantAccount = "three_d_secure_merchant_account";
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

    public TestClientTokenBuilder withTouchEnabled() {
        mTouchEnabled = true;
        return this;
    }

    public TestClientTokenBuilder withOfflineVenmo() {
        mVenmoEnvironment = "offline";
        return this;
    }

    public TestClientTokenBuilder withLiveVenmo() {
        mVenmoEnvironment = "live";
        return this;
    }

    public String build() {
        switch (mMerchantType) {
            case MERCHANT_WITHOUT_PAYPAL:
                return getClientTokenFromGateway("integration2_merchant_id", "integration2_public_key");
            case MERCHANT_WITH_FAKE_PAYPAL:
            case MERCHANT_WITH_PAYPAL:
            case MERCHANT_WITH_THREE_D_SECURE_ENABLED:
                return getClientTokenFromGateway("integration_merchant_id", "integration_public_key");
            case MERCHANT_WITH_COINBASE_ENABLED:
                enableCoinbase(true);
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
        try {
            JSONObject json = new JSONObject();
            json.put("public_key", merchantPublicKey);
            json.put("customer", "true");
            json.put("token_version", "2");

            if (!mWithCustomer) {
                json.put("no_customer", "true");
            }
            if (mAnalytics) {
                json.put("analytics", "true");
            }

            if (mMerchantAccount != null) {
                json.put("merchant_account_id", mMerchantAccount);
            }

            JSONObject overrides = new JSONObject();
            JSONArray challenges = new JSONArray();
            for (String challenge : mChallenges) {
                challenges.put(challenge);
            }
            overrides.put("challenges", challenges);

            if (!mTouchEnabled) {
                overrides.put("paypal", new JSONObject().put("touchDisabled", "true"));
            }

            if (mVenmoEnvironment != null) {
                overrides.put("venmo", mVenmoEnvironment);
            }

            json.put("overrides", overrides);

            String path = "/merchants/" + merchantId + "/client_api/testing/client_token";
            Response response = TestClientTokenBuilder.request(path, "POST", json.toString());

            String encodedToken = new JSONObject(response.getBody()).getString("clientToken");
            JSONObject clientTokenJson = new JSONObject(new String(Base64.decode(encodedToken, Base64.DEFAULT)));
            if (mVenmoEnvironment == null) {
                clientTokenJson.put("venmo", "off");
            }
            if (mMerchantType == MERCHANT_WITH_FAKE_PAYPAL) {
                clientTokenJson.put("paypal", clientTokenJson.getJSONObject("paypal").put("environment", "offline"));
            }
            return clientTokenJson.toString();
        } catch (JSONException e) {
            throw new RuntimeException("There was an error building your json request: " + e.getMessage());
        }
    }

    public static void enableCoinbase(boolean enabled) {
        try {
            JSONObject json = new JSONObject();
            json.put("public_key", "integration_public_key");
            json.put("coinbase_merchant_options", new JSONObject().put("enabled", enabled));

            String path = "/merchants/integration_merchant_id/client_api/testing/mock_coinbase_merchant_options";
            Response response = TestClientTokenBuilder.request(path, "PUT", json.toString());

            if (response.getCode() != 200) {
                throw new RuntimeException("Setting Coinbase to " + enabled + " failed with response" +
                        " code " + response.getCode() + " for merchant integration_merchant_id");
            }
        } catch (JSONException e) {
            throw new RuntimeException("There was an error building your json request: " + e.getMessage());
        }
    }

    private static Response request(String path, String requestMethod, String payload) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(EnvironmentHelper.getGatewayPath() + path);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(requestMethod);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.writeBytes(payload);
            out.flush();
            out.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder responseBody = new StringBuilder();
            while ((line = in.readLine()) != null) {
                responseBody.append(line);
            }
            in.close();

            return new Response(connection.getResponseCode(), responseBody.toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException("The url to your Gateway was invalid");
        } catch (IOException e) {
            throw new RuntimeException("There was an error connecting to your Gateway: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static class Response {
        private int mCode;
        private String mBody;

        Response(int code, String body) {
            mCode = code;
            mBody = body;
        }

        public int getCode() {
            return mCode;
        }

        public String getBody() {
            return mBody;
        }
    }
}
