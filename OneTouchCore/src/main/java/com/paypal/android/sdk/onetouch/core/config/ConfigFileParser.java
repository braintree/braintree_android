package com.paypal.android.sdk.onetouch.core.config;

import com.paypal.android.networking.EnvironmentManager;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ConfigFileParser {
    private static final String TAG = ConfigFileParser.class.getSimpleName();

    public OtcConfiguration getParsedConfig(JSONObject rootObject) throws JSONException {
        OtcConfiguration otcConfiguration = new OtcConfiguration();
        String os = rootObject.getString("os");

        String file_timestamp = rootObject.getString("file_timestamp");
        otcConfiguration.fileTimestamp(file_timestamp);

        JSONObject oneDotZeroConfig = rootObject.getJSONObject("1.0");

        //parse oauth2 configs
        JSONArray oauth2_recipes_in_decreasing_priority_order =
                oneDotZeroConfig.getJSONArray("oauth2_recipes_in_decreasing_priority_order");

        for (int i = 0; i < oauth2_recipes_in_decreasing_priority_order.length(); i++) {
            JSONObject oauth2_recipe = oauth2_recipes_in_decreasing_priority_order.getJSONObject(i);
            if (null != oauth2_recipe) {
                OAuth2Recipe recipe = getOAuth2Recipe(oauth2_recipe);
                otcConfiguration.withOauth2Recipe(recipe);
            }
        }

        //parse checkout configs
        JSONArray checkout_recipes_in_decreasing_priority_order =
                oneDotZeroConfig.getJSONArray("checkout_recipes_in_decreasing_priority_order");

        for (int i = 0; i < checkout_recipes_in_decreasing_priority_order.length(); i++) {
            JSONObject checkout_recipe =
                    checkout_recipes_in_decreasing_priority_order.getJSONObject(i);
            if (null != checkout_recipe) {
                CheckoutRecipe recipe = getCheckoutRecipe(checkout_recipe);
                otcConfiguration.withCheckoutRecipe(recipe);
            }
        }

        //parse billing agreement configs
        JSONArray billing_agreement_recipes_in_decreasing_priority_order =
                oneDotZeroConfig
                        .getJSONArray("billing_agreement_recipes_in_decreasing_priority_order");

        for (int i = 0; i < billing_agreement_recipes_in_decreasing_priority_order.length(); i++) {
            JSONObject ba_recipe =
                    billing_agreement_recipes_in_decreasing_priority_order.getJSONObject(i);
            if (null != ba_recipe) {
                BillingAgreementRecipe recipe = getBillingAgreementRecipe(ba_recipe);
                otcConfiguration.withBillingAgreementRecipe(recipe);
            }
        }

        return otcConfiguration;
    }

    private CheckoutRecipe getCheckoutRecipe(JSONObject checkout_recipe) throws JSONException {
        CheckoutRecipe recipe = new CheckoutRecipe();
        populateCommonData(recipe, checkout_recipe);

        return recipe;
    }

    private BillingAgreementRecipe getBillingAgreementRecipe(JSONObject ba_recipe)
            throws JSONException {
        BillingAgreementRecipe recipe = new BillingAgreementRecipe();
        populateCommonData(recipe, ba_recipe);

        return recipe;
    }

    private void populateCommonData(Recipe<?> recipe, JSONObject json_recipe) throws JSONException {
        recipe.target(RequestTarget.valueOf(json_recipe.getString("target")))
                .protocol(json_recipe.getString("protocol"));

        if (json_recipe.has("component")) {
            recipe.targetComponent(json_recipe.getString("component"));
        }

        // intent actions are not specified in browser target
        if (json_recipe.has("intent_action")) {
            recipe.targetIntentAction(json_recipe.getString("intent_action"));
        }

        JSONArray packagesArray = json_recipe.getJSONArray("packages");
        for (int j = 0; j < packagesArray.length(); j++) {
            String packageValue = packagesArray.getString(j);
            recipe.targetPackage(packageValue);
        }

        if (json_recipe.has("supported_locales")) {
            JSONArray supportedLocalesArray = json_recipe.getJSONArray("supported_locales");
            for (int j = 0; j < supportedLocalesArray.length(); j++) {
                String supportedLocale = supportedLocalesArray.getString(j);
                recipe.supportedLocale(supportedLocale);
            }
        }
    }

    private OAuth2Recipe getOAuth2Recipe(JSONObject json_oauth2_recipe) throws JSONException {
        OAuth2Recipe recipe = new OAuth2Recipe();

        populateCommonData(recipe, json_oauth2_recipe);

        JSONArray scopeArray = json_oauth2_recipe.getJSONArray("scope");

        for (int j = 0; j < scopeArray.length(); j++) {
            String scopeValue = scopeArray.getString(j);
            if ("*".equals(scopeValue)) {
                recipe.validForAllScopes();
            } else {
                recipe.validForScope(scopeValue);
            }
        }

        if (json_oauth2_recipe.has("endpoints")) {
            JSONObject endpoints = json_oauth2_recipe.getJSONObject("endpoints");

            String name;
            JSONObject jsonEnvironment;
            if (endpoints.has(EnvironmentManager.LIVE)) {
                name = EnvironmentManager.LIVE;
                jsonEnvironment = endpoints.getJSONObject(EnvironmentManager.LIVE);
                addEnvironment(recipe, name, jsonEnvironment);
            }

            if (endpoints.has(OAuth2Recipe.DEVELOP)) {
                name = OAuth2Recipe.DEVELOP;
                jsonEnvironment = endpoints.getJSONObject(OAuth2Recipe.DEVELOP);
                addEnvironment(recipe, name, jsonEnvironment);
            }

            if (endpoints.has(EnvironmentManager.MOCK)) {
                name = EnvironmentManager.MOCK;
                jsonEnvironment = endpoints.getJSONObject(EnvironmentManager.MOCK);
                addEnvironment(recipe, name, jsonEnvironment);
            }
        }
        return recipe;
    }

    private void addEnvironment(OAuth2Recipe recipe, String name, JSONObject jsonEnvironment)
            throws JSONException {
        ConfigEndpoint endpoint = new ConfigEndpoint()
                .name(name)
                .url(jsonEnvironment.getString("url"))
                .certificate(jsonEnvironment.getString("certificate"));
        recipe.withEndpoint(name, endpoint);
    }
}
