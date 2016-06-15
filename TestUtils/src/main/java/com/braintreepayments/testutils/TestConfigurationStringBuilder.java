package com.braintreepayments.testutils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class TestConfigurationStringBuilder extends JSONBuilder {

    public TestConfigurationStringBuilder() {
        super();
        clientApiUrl("client_api_url");
        environment("test");
        merchantId("integration_merchant_id");
    }

    public TestConfigurationStringBuilder clientApiUrl(String clientApiUrl) {
        put(clientApiUrl);
        return this;
    }

    public TestConfigurationStringBuilder challenges(String... challenges) {
        JSONArray challengesJson = new JSONArray();
        for (String challenge : challenges) {
            challengesJson.put(challenge);
        }
        put(challengesJson);
        return this;
    }

    public TestConfigurationStringBuilder environment(String environment) {
        put(environment);
        return this;
    }

    public TestConfigurationStringBuilder merchantId(String merchantId) {
        put(merchantId);
        return this;
    }

    public TestConfigurationStringBuilder merchantAccountId(String merchantAccountId) {
        put(merchantAccountId);
        return this;
    }

    public TestConfigurationStringBuilder paypalEnabled(boolean paypalEnabled) {
        put(Boolean.toString(paypalEnabled));
        paypal(new TestPayPalConfigurationBuilder()
                .environment("test")
                .displayName("displayName")
                .clientId("clientId")
                .privacyUrl("http://privacy.com")
                .userAgreementUrl("http://user.agreement.com"));

        return this;
    }

    public TestConfigurationStringBuilder threeDSecureEnabled(boolean threeDSecureEnabled) {
        put(Boolean.toString(threeDSecureEnabled));
        return this;
    }

    public TestConfigurationStringBuilder analytics(String analyticsUrl) {
        try {
            JSONObject analyticsJson = new JSONObject();
            analyticsJson.put("url", analyticsUrl);
            put(analyticsJson);
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationStringBuilder paypal(TestPayPalConfigurationBuilder builder) {
        try {
            put(new JSONObject(builder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationStringBuilder androidPay(TestAndroidPayConfigurationBuilder builder) {
        try {
            put(new JSONObject(builder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationStringBuilder payWithVenmo(TestVenmoConfigurationBuilder venmoConfigurationBuilder) {
        try {
            put(new JSONObject(venmoConfigurationBuilder.build()));
        } catch(JSONException ignored) {}
        return this;
    }

    public TestConfigurationStringBuilder kount(TestKountConfigurationBuilder kountConfigurationBuilder) {
        try {
            put(new JSONObject(kountConfigurationBuilder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public static class TestVenmoConfigurationBuilder extends JSONBuilder {

        public TestVenmoConfigurationBuilder accessToken(String accessToken) {
            put(accessToken);
            return this;
        }

        public TestVenmoConfigurationBuilder merchantId(String merchantId) {
            put(merchantId);
            return this;
        }

        public TestVenmoConfigurationBuilder environment(String environment) {
            put(environment);
            return this;
        }
    }

    public static class TestPayPalConfigurationBuilder extends JSONBuilder {

        public TestPayPalConfigurationBuilder displayName(String displayName) {
            put(displayName);
            return this;
        }

        public TestPayPalConfigurationBuilder clientId(String clientId) {
            put(clientId);
            return this;
        }

        public TestPayPalConfigurationBuilder privacyUrl(String privacyUrl) {
            put(privacyUrl);
            return this;
        }

        public TestPayPalConfigurationBuilder userAgreementUrl(String userAgreementUrl) {
            put(userAgreementUrl);
            return this;
        }

        public TestPayPalConfigurationBuilder directBaseUrl(String directBaseUrl) {
            put(directBaseUrl);
            return this;
        }

        public TestPayPalConfigurationBuilder environment(String environment) {
            put(environment);
            return this;
        }

        public TestPayPalConfigurationBuilder touchDisabled(boolean touchDisabled) {
            put(Boolean.toString(touchDisabled));
            return this;
        }

        public TestPayPalConfigurationBuilder currencyIsoCode(String currencyIsoCode) {
            put(currencyIsoCode);
            return this;
        }

        public TestPayPalConfigurationBuilder billingAgreementsEnabled(boolean billingAgreementsEnabled) {
            put(Boolean.toString(billingAgreementsEnabled));
            return this;
        }
    }

    public static class TestAndroidPayConfigurationBuilder extends JSONBuilder {

        public TestAndroidPayConfigurationBuilder enabled(boolean enabled) {
            put(Boolean.toString(enabled));
            return this;
        }

        public TestAndroidPayConfigurationBuilder googleAuthorizationFingerprint(String fingerprint) {
            put(fingerprint);
            return this;
        }

        public TestAndroidPayConfigurationBuilder environment(String environment) {
            put(environment);
            return this;
        }

        public TestAndroidPayConfigurationBuilder displayName(String dislayName) {
            put(dislayName);
            return this;
        }

        public TestAndroidPayConfigurationBuilder supportedNetworks(String[] supportedNetworks) {
            put(new JSONArray(Arrays.asList(supportedNetworks)));
            return this;
        }
    }

    public static class TestKountConfigurationBuilder extends JSONBuilder {

        public TestKountConfigurationBuilder enabled(boolean enabled) {
            put(enabled);
            return this;
        }

        public TestKountConfigurationBuilder kountMerchantId(String kountMerchantid) {
            put(kountMerchantid);
            return this;
        }
    }
}
