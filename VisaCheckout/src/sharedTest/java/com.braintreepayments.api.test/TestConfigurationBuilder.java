package com.braintreepayments.api.test;

import com.braintreepayments.api.models.GraphQLConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class TestConfigurationBuilder extends JSONBuilder {

    public static <T> T basicConfig() {
        return new TestConfigurationBuilder().buildConfiguration();
    }

    public TestConfigurationBuilder() {
        super();
        clientApiUrl("client_api_url");
        environment("test");
        merchantId("integration_merchant_id");
    }

    public TestConfigurationBuilder assetsUrl(String assetsUrl) {
        put(assetsUrl);
        return this;
    }

    public TestConfigurationBuilder clientApiUrl(String clientApiUrl) {
        put(clientApiUrl);
        return this;
    }

    public TestConfigurationBuilder challenges(String... challenges) {
        JSONArray challengesJson = new JSONArray();
        for (String challenge : challenges) {
            challengesJson.put(challenge);
        }
        put(challengesJson);
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

    public TestConfigurationBuilder threeDSecureEnabled(boolean threeDSecureEnabled) {
        put(Boolean.toString(threeDSecureEnabled));
        return this;
    }

    public TestConfigurationBuilder withAnalytics() {
        analytics("http://example.com");
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
            paypalEnabled(true);
            put(new JSONObject(builder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder paypalEnabled(boolean enabled) {
        put(enabled);

        if (enabled) {
            try {
                put("paypal", new JSONObject(new TestPayPalConfigurationBuilder(true).build()));
            } catch (JSONException ignored) {}
        }

        return this;
    }

    public TestConfigurationBuilder androidPay(TestAndroidPayConfigurationBuilder builder) {
        try {
            put(new JSONObject(builder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder payWithVenmo(TestVenmoConfigurationBuilder venmoConfigurationBuilder) {
        try {
            put(new JSONObject(venmoConfigurationBuilder.build()));
        } catch(JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder kount(TestKountConfigurationBuilder kountConfigurationBuilder) {
        try {
            put(new JSONObject(kountConfigurationBuilder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder visaCheckout(TestVisaCheckoutConfigurationBuilder visaCheckoutConfigurationBuilder) {
        try {
            put(new JSONObject(visaCheckoutConfigurationBuilder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder braintreeApi(TestBraintreeApiConfigurationBuilder braintreeApiConfigurationBuilder) {
        try {
            put(new JSONObject(braintreeApiConfigurationBuilder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder graphQL() {
        try {
            JSONObject graphQLJson = new JSONObject();
            graphQLJson.put("url", "http://10.0.2.2:8080/graphql");
            graphQLJson.put("features", new JSONArray().put(GraphQLConfiguration.TOKENIZE_CREDIT_CARDS_FEATURE));
            put(graphQLJson);
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder ideal(TestIdealConfigurationBuilder idealConfigurationBuilder) {
        try {
            put(new JSONObject(idealConfigurationBuilder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder graphQL(TestGraphQLConfigurationBuilder graphQLConfigurationBuilder) {
        try {
            put(new JSONObject(graphQLConfigurationBuilder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T buildConfiguration() {
        try {
            Class configuration = Class.forName("com.braintreepayments.api.models.Configuration");
            Method fromJson = configuration.getDeclaredMethod("fromJson", String.class);
            return (T) fromJson.invoke(null, build());
        } catch (NoSuchMethodException ignored) {}
        catch (InvocationTargetException ignored) {}
        catch (IllegalAccessException ignored) {}
        catch (ClassNotFoundException ignored) {}

        return (T) build();
    }

    public TestVenmoConfigurationBuilder payWithVenmo() {
        try {
            return new TestVenmoConfigurationBuilder(mJsonBody.getJSONObject("payWithVenmo"));
        } catch (JSONException ignored) {}
        return new TestVenmoConfigurationBuilder();
    }

    public TestAndroidPayConfigurationBuilder androidPay() {
        try {
            return new TestAndroidPayConfigurationBuilder(mJsonBody.getJSONObject("androidPay"));
        } catch (JSONException ignored) {}
        return new TestAndroidPayConfigurationBuilder();
    }

    public TestPayPalConfigurationBuilder paypal() {
        try {
            return new TestPayPalConfigurationBuilder(mJsonBody.getJSONObject("paypal"));
        } catch (JSONException ignored) {}
        return new TestPayPalConfigurationBuilder(true);
    }

    public TestKountConfigurationBuilder kount() {
        try {
            return new TestKountConfigurationBuilder(mJsonBody.getJSONObject("kount"));
        } catch (JSONException ignored) {}
        return new TestKountConfigurationBuilder();
    }

    public TestVisaCheckoutConfigurationBuilder visaCheckout() {
        try {
            return new TestVisaCheckoutConfigurationBuilder(mJsonBody.getJSONObject("visaCheckout"));
        } catch (JSONException ignored) {}
        return new TestVisaCheckoutConfigurationBuilder();
    }

    public TestGraphQLConfigurationBuilder graphQLConfigurationBuilder() {
        try {
            return new TestGraphQLConfigurationBuilder(mJsonBody.getJSONObject("graphQL"));
        } catch (JSONException ignored) {}
        return new TestGraphQLConfigurationBuilder();
    }

    public static class TestVenmoConfigurationBuilder extends JSONBuilder {

        public TestVenmoConfigurationBuilder() {
            super();
        }

        protected TestVenmoConfigurationBuilder(JSONObject json) {
            super(json);
        }

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

        public TestPayPalConfigurationBuilder(boolean enabled) {
            super();

            if (enabled) {
                environment("test");
                displayName("displayName");
                clientId("clientId");
                privacyUrl("http://privacy.gov");
                userAgreementUrl("http://i.agree.biz");
            }
        }

        protected TestPayPalConfigurationBuilder(JSONObject json) {
            super(json);
        }

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

        public TestAndroidPayConfigurationBuilder() {
            super();
        }

        protected TestAndroidPayConfigurationBuilder(JSONObject json) {
            super(json);
        }

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

        public TestKountConfigurationBuilder() {
            super();
        }

        protected TestKountConfigurationBuilder(JSONObject json) {
            super(json);
        }

        public TestKountConfigurationBuilder kountMerchantId(String kountMerchantid) {
            put(kountMerchantid);
            return this;
        }
    }

    public static class TestVisaCheckoutConfigurationBuilder extends JSONBuilder {

        public TestVisaCheckoutConfigurationBuilder() {
            super();
        }

        protected TestVisaCheckoutConfigurationBuilder(JSONObject json) {
            super(json);
        }

        public TestVisaCheckoutConfigurationBuilder apikey(String apikey) {
            put(apikey);
            return this;
        }

        public TestVisaCheckoutConfigurationBuilder externalClientId(String externalClientId) {
            put(externalClientId);
            return this;
        }

        public TestVisaCheckoutConfigurationBuilder supportedCardTypes(String... supportedCardTypes) {
            put(new JSONArray(Arrays.asList(supportedCardTypes)));
            return this;
        }
    }

    public static class TestBraintreeApiConfigurationBuilder extends JSONBuilder {

        public TestBraintreeApiConfigurationBuilder() {
            super();
        }

        protected TestBraintreeApiConfigurationBuilder(JSONObject json) {
            super(json);
        }

        public TestBraintreeApiConfigurationBuilder accessToken(String accessToken) {
            put(accessToken);
            return this;
        }

        public TestBraintreeApiConfigurationBuilder url(String url) {
            put(url);
            return this;
        }
    }

    public static class TestGraphQLConfigurationBuilder extends JSONBuilder {

        public TestGraphQLConfigurationBuilder() {
            super();
        }

        protected TestGraphQLConfigurationBuilder(JSONObject json) {
            super(json);
        }

        public TestGraphQLConfigurationBuilder url(String url) {
            put(url);
            return this;
        }

        public TestGraphQLConfigurationBuilder features(String... features) {
            JSONArray jsonFeatures = new JSONArray();
            for (String feature : features) {
                jsonFeatures.put(feature);
            }

            put(jsonFeatures);
            return this;
        }
    }

    public static class TestIdealConfigurationBuilder extends JSONBuilder {

        public TestIdealConfigurationBuilder() {
            super();
        }

        protected TestIdealConfigurationBuilder(JSONObject json) {
            super(json);
        }

        public TestIdealConfigurationBuilder routeId(String routeId) {
            put(routeId);
            return this;
        }

        public TestIdealConfigurationBuilder assetsUrl(String assetsUrl) {
            put(assetsUrl);
            return this;
        }
    }
}
