package com.braintreepayments.api.test;

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
    private static final int MERCHANT_WITH_CVV_VERIFICATION = 2;
    private static final int MERCHANT_WITH_POSTAL_CODE_VERIFICATION = 3;
    private static final int MERCHANT_WITH_CVV_AND_POSTAL_CODE_VERIFICATION = 4;
    private static final int MERCHANT_WITH_THREE_D_SECURE_ENABLED = 5;
    private static final int MERCHANT_WITH_UNIONPAY = 6;

    private boolean mWithCustomer = true;
    private int mMerchantType = MERCHANT_WITHOUT_PAYPAL;
    private String mMerchantAccount = null;
    private boolean mAnalytics = false;
    private ArrayList<String> mChallenges = new ArrayList<String>() {{ add("cvv"); add("postal_code"); }};
    private boolean mRevoked = false;
    private boolean mTouchEnabled = false;
    private boolean mWithFakePayPal = true;

    public TestClientTokenBuilder withoutCustomer() {
        mWithCustomer = false;
        return this;
    }

    public TestClientTokenBuilder withPayPal() {
        mMerchantType = MERCHANT_WITH_PAYPAL;
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

    public TestClientTokenBuilder withUnionPay() {
        mMerchantType = MERCHANT_WITH_UNIONPAY;
        mMerchantAccount = "fake_switch_usd";
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

    public TestClientTokenBuilder withTouchEnabled() {
        mTouchEnabled = true;
        return this;
    }

    public TestClientTokenBuilder withPayPalStage() {
        mWithFakePayPal = false;
        return this;
    }

    public TestClientTokenBuilder withVisaCheckout() {
        withCvvVerification();
        return this;
    }

    public String build() {
        switch (mMerchantType) {
            case MERCHANT_WITHOUT_PAYPAL:
                return getClientTokenFromGateway("integration2_merchant_id", "integration2_public_key");
            case MERCHANT_WITH_PAYPAL:
            case MERCHANT_WITH_THREE_D_SECURE_ENABLED:
            case MERCHANT_WITH_UNIONPAY:
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

            if (mRevoked) {
                overrides.put("options", new JSONObject().put("revoked", "true"));
            }

            if (!mTouchEnabled) {
                overrides.put("paypal", new JSONObject().put("touchDisabled", "true"));
            }

            json.put("overrides", overrides);

            String path = "/merchants/" + merchantId + "/client_api/testing/client_token";
            Response response = TestClientTokenBuilder.request(path, "POST", json.toString());

            String encodedToken = new JSONObject(response.getBody()).getString("clientToken");
            String clientToken = new String(Base64.decode(encodedToken, Base64.DEFAULT));

            String replacement;
            if (EnvironmentHelper.getGatewayIp().startsWith("http")) {
                replacement = "http://localhost";
            } else {
                replacement = "localhost";
            }
            clientToken = clientToken.replaceAll(replacement, EnvironmentHelper.getGatewayIp());

            JSONObject clientTokenJson = new JSONObject(clientToken);
            if (mWithFakePayPal) {
                clientTokenJson.put("paypal", clientTokenJson.getJSONObject("paypal").put("environment", "offline"));
            }
            return clientTokenJson.toString();
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
