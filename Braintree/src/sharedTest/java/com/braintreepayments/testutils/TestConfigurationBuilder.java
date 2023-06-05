package com.braintreepayments.testutils;

import com.braintreepayments.api.internal.GraphQLConstants.Features;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

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

    public TestConfigurationBuilder cardinalAuthenticationJWT(String jwt) {
        put(jwt);
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

    public TestConfigurationBuilder googlePayment(TestGooglePaymentConfigurationBuilder builder) {
        try {
            put("androidPay", new JSONObject(builder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder payWithVenmo(TestVenmoConfigurationBuilder venmoConfigurationBuilder) {
        try {
            put(new JSONObject(venmoConfigurationBuilder.build()));
        } catch(JSONException ignored) {}
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
            graphQLJson.put("features", new JSONArray().put(Features.TOKENIZE_CREDIT_CARDS));
            put(graphQLJson);
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder graphQL(TestGraphQLConfigurationBuilder graphQLConfigurationBuilder) {
        try {
            put(new JSONObject(graphQLConfigurationBuilder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder samsungPay(TestSamsungPayConfigurationBuilder samsungPayConfigurationBuilder) {
        try {
            put(new JSONObject(samsungPayConfigurationBuilder.build()));
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

    public TestGooglePaymentConfigurationBuilder googlePayment() {
        try {
            return new TestGooglePaymentConfigurationBuilder(mJsonBody.getJSONObject("androidPay"));
        } catch (JSONException ignored) {}
        return new TestGooglePaymentConfigurationBuilder();
    }

    public TestPayPalConfigurationBuilder paypal() {
        try {
            return new TestPayPalConfigurationBuilder(mJsonBody.getJSONObject("paypal"));
        } catch (JSONException ignored) {}
        return new TestPayPalConfigurationBuilder(true);
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

    public TestSamsungPayConfigurationBuilder samsungPayConfigurationBuilder() {
        try {
            return new TestSamsungPayConfigurationBuilder(mJsonBody.getJSONObject("samsungPay"));
        } catch (JSONException ignored) {}
        return new TestSamsungPayConfigurationBuilder();
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

    public static class TestGooglePaymentConfigurationBuilder extends JSONBuilder {

        public TestGooglePaymentConfigurationBuilder() {
            super();
        }

        protected TestGooglePaymentConfigurationBuilder(JSONObject json) {
            super(json);
        }

        public TestGooglePaymentConfigurationBuilder enabled(boolean enabled) {
            put(Boolean.toString(enabled));
            return this;
        }

        public TestGooglePaymentConfigurationBuilder googleAuthorizationFingerprint(String fingerprint) {
            put(fingerprint);
            return this;
        }

        public TestGooglePaymentConfigurationBuilder environment(String environment) {
            put(environment);
            return this;
        }

        public TestGooglePaymentConfigurationBuilder displayName(String dislayName) {
            put(dislayName);
            return this;
        }

        public TestGooglePaymentConfigurationBuilder supportedNetworks(String[] supportedNetworks) {
            put(new JSONArray(Arrays.asList(supportedNetworks)));
            return this;
        }

        public TestGooglePaymentConfigurationBuilder paypalClientId(String paypalClientId) {
            put(paypalClientId);
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

    public static class TestSamsungPayConfigurationBuilder extends JSONBuilder {

        public TestSamsungPayConfigurationBuilder() {
            super();
        }

        protected TestSamsungPayConfigurationBuilder(JSONObject json) {
            super(json);
        }

        public TestSamsungPayConfigurationBuilder merchantDisplayName(String displayName) {
            put(displayName);
            return this;
        }

        public TestSamsungPayConfigurationBuilder serviceId(String serviceId) {
            put(serviceId);
            return this;
        }

        public TestSamsungPayConfigurationBuilder supportedCardBrands(List<String> supportedCardBrands) {
            JSONArray jsonBrands = new JSONArray();
            for (String feature : supportedCardBrands) {
                jsonBrands.put(feature);
            }

            put(jsonBrands);
            return this;
        }

        public TestSamsungPayConfigurationBuilder samsungAuthorization(String authorization) {
            put(authorization);
            return this;
        }
    }
}
