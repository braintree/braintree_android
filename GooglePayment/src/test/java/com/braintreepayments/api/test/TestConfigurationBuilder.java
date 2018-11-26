package com.braintreepayments.api.test;

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

    public TestConfigurationBuilder clientApiUrl(String clientApiUrl) {
        put(clientApiUrl);
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

    public TestConfigurationBuilder visaCheckout(TestVisaCheckoutConfigurationBuilder visaCheckoutConfigurationBuilder) {
        try {
            put(new JSONObject(visaCheckoutConfigurationBuilder.build()));
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
}
