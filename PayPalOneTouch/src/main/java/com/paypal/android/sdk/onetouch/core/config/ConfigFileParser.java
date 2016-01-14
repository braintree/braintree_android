package com.paypal.android.sdk.onetouch.core.config;

import com.paypal.android.sdk.onetouch.core.network.EnvironmentManager;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class ConfigFileParser {

    OtcConfiguration getParsedConfig(JSONObject rootObject) throws JSONException {
        OtcConfiguration otcConfiguration = new OtcConfiguration();
        otcConfiguration.fileTimestamp(rootObject.getString("file_timestamp"));

        JSONObject oneDotZeroConfig = rootObject.getJSONObject("1.0");

        JSONArray oauth2RecipesInDecreasingPriorityOrder =
                oneDotZeroConfig.getJSONArray("oauth2_recipes_in_decreasing_priority_order");

        for (int i = 0; i < oauth2RecipesInDecreasingPriorityOrder.length(); i++) {
            JSONObject oauth2Recipe = oauth2RecipesInDecreasingPriorityOrder.getJSONObject(i);
            if (null != oauth2Recipe) {
                OAuth2Recipe recipe = getOAuth2Recipe(oauth2Recipe);
                otcConfiguration.withOauth2Recipe(recipe);
            }
        }

        JSONArray checkoutRecipesInDecreasingPriorityOrder =
                oneDotZeroConfig.getJSONArray("checkout_recipes_in_decreasing_priority_order");

        for (int i = 0; i < checkoutRecipesInDecreasingPriorityOrder.length(); i++) {
            JSONObject checkoutRecipe = checkoutRecipesInDecreasingPriorityOrder.getJSONObject(i);
            if (null != checkoutRecipe) {
                CheckoutRecipe recipe = getCheckoutRecipe(checkoutRecipe);
                otcConfiguration.withCheckoutRecipe(recipe);
            }
        }

        JSONArray billingAgreementRecipesInDecreasingPriorityOrder =
                oneDotZeroConfig.getJSONArray("billing_agreement_recipes_in_decreasing_priority_order");

        for (int i = 0; i < billingAgreementRecipesInDecreasingPriorityOrder.length(); i++) {
            JSONObject billingAgreementRecipe = billingAgreementRecipesInDecreasingPriorityOrder.getJSONObject(i);
            if (null != billingAgreementRecipe) {
                BillingAgreementRecipe recipe = getBillingAgreementRecipe(billingAgreementRecipe);
                otcConfiguration.withBillingAgreementRecipe(recipe);
            }
        }

        return otcConfiguration;
    }

    private CheckoutRecipe getCheckoutRecipe(JSONObject checkoutRecipe) throws JSONException {
        CheckoutRecipe recipe = new CheckoutRecipe();
        populateCommonData(recipe, checkoutRecipe);

        return recipe;
    }

    private BillingAgreementRecipe getBillingAgreementRecipe(JSONObject billingAgreementRecipe) throws JSONException {
        BillingAgreementRecipe recipe = new BillingAgreementRecipe();
        populateCommonData(recipe, billingAgreementRecipe);

        return recipe;
    }

    private void populateCommonData(Recipe<?> recipe, JSONObject jsonRecipe) throws JSONException {
        recipe.target(RequestTarget.valueOf(jsonRecipe.getString("target")))
                .protocol(jsonRecipe.getString("protocol"));

        if (jsonRecipe.has("component")) {
            recipe.targetComponent(jsonRecipe.getString("component"));
        }

        if (jsonRecipe.has("intent_action")) {
            recipe.targetIntentAction(jsonRecipe.getString("intent_action"));
        }

        JSONArray packagesArray = jsonRecipe.getJSONArray("packages");
        for (int j = 0; j < packagesArray.length(); j++) {
            String packageValue = packagesArray.getString(j);
            recipe.targetPackage(packageValue);
        }

        if (jsonRecipe.has("supported_locales")) {
            JSONArray supportedLocalesArray = jsonRecipe.getJSONArray("supported_locales");
            for (int j = 0; j < supportedLocalesArray.length(); j++) {
                String supportedLocale = supportedLocalesArray.getString(j);
                recipe.supportedLocale(supportedLocale);
            }
        }
    }

    private OAuth2Recipe getOAuth2Recipe(JSONObject jsonOauth2Recipe) throws JSONException {
        OAuth2Recipe recipe = new OAuth2Recipe();

        populateCommonData(recipe, jsonOauth2Recipe);

        JSONArray scopeArray = jsonOauth2Recipe.getJSONArray("scope");

        for (int j = 0; j < scopeArray.length(); j++) {
            String scopeValue = scopeArray.getString(j);
            if ("*".equals(scopeValue)) {
                recipe.validForAllScopes();
            } else {
                recipe.validForScope(scopeValue);
            }
        }

        if (jsonOauth2Recipe.has("endpoints")) {
            JSONObject endpoints = jsonOauth2Recipe.getJSONObject("endpoints");

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

    private void addEnvironment(OAuth2Recipe recipe, String name, JSONObject jsonEnvironment) throws JSONException {
        recipe.withEndpoint(name,
                new ConfigEndpoint(name, jsonEnvironment.getString("url"), jsonEnvironment.getString("certificate")));
    }
}
