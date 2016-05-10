package com.braintreepayments.testutils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class TestConfigurationBuilder extends JSONBuilder {

    public TestConfigurationBuilder() {
        super();
        clientApiUrl("client_api_url");
        environment("test");
        merchantId("integration_merchant_id");
    }

    public TestConfigurationBuilder clientApiUrl(String clientApiUrl) {
        put(clientApiUrl);
        return this;
    }

    public TestConfigurationBuilder challenges(String... challenges) {
        put(Arrays.toString(challenges));
        return this;
    }

    public TestConfigurationBuilder environment(String environment) {
        put(environment);
        return this;
    }

    public TestConfigurationBuilder merchantId(String merchantId) {
        put(merchantId);
        return this;
    }

    public TestConfigurationBuilder merchantAccountId(String merchantAccountId) {
        put(merchantAccountId);
        return this;
    }

    public TestConfigurationBuilder paypalEnabled(boolean paypalEnabled) {
        put(Boolean.toString(paypalEnabled));
        paypal(new TestPayPalConfigurationBuilder()
                .environment("test")
                .displayName("displayName")
                .clientId("clientId")
                .privacyUrl("http://privacy.com")
                .userAgreementUrl("http://user.agreement.com"));

        return this;
    }

    public TestConfigurationBuilder threeDSecureEnabled(boolean threeDSecureEnabled) {
        put(Boolean.toString(threeDSecureEnabled));
        return this;
    }

    public TestConfigurationBuilder analytics(String analyticsUrl) {
        try {
            JSONObject analyticsJson = new JSONObject();
            analyticsJson.put("url", analyticsUrl);
            put(analyticsJson);
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder paypal(TestPayPalConfigurationBuilder builder) {
        try {
            put(new JSONObject(builder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder androidPay(TestAndroidPayConfigurationBuilder builder) {
        try {
            put(new JSONObject(builder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder payWithVenmo(String accessToken) {
        try {
            JSONObject venmoJson = new JSONObject();
            venmoJson.put("accessToken", accessToken);
            put(venmoJson);
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder kount(TestKountConfigurationBuilder kountConfigurationBuilder) {
        try {
            put(new JSONObject(kountConfigurationBuilder.build()));
        } catch (JSONException ignored) {}
        return this;
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
