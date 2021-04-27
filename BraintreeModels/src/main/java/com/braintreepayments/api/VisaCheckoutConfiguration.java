package com.braintreepayments.api;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Contains the remote Visa Checkout configuration for the Braintree SDK.
 */
class VisaCheckoutConfiguration {

    private boolean isEnabled;
    private String apiKey;
    private String externalClientId;
    private List<String> cardBrands;

    static VisaCheckoutConfiguration fromJson(JSONObject json) {
        VisaCheckoutConfiguration visaCheckoutConfiguration = new VisaCheckoutConfiguration();

        if (json == null) {
            json = new JSONObject();
        }

        visaCheckoutConfiguration.apiKey = Json.optString(json, "apikey", "");
        visaCheckoutConfiguration.isEnabled = !visaCheckoutConfiguration.apiKey.equals("");
        visaCheckoutConfiguration.externalClientId = Json.optString(json, "externalClientId", "");
        visaCheckoutConfiguration.cardBrands = supportedCardTypesToAcceptedCardBrands(
                CardConfiguration.fromJson(json).getSupportedCardTypes());

        return visaCheckoutConfiguration;
    }

    /**
     * Determines if the Visa Checkout flow is available to be used. This can be used to determine
     * if UI components should be shown or hidden.
     *
     * @return boolean if Visa Checkout SDK is available, and configuration is enabled.
     */
    boolean isEnabled() {
        return isEnabled;
    }

    /**
     * @return The Visa Checkout External Client Id associated with this merchant's Visa Checkout configuration.
     */
    String getExternalClientId() {
        return externalClientId;
    }

    /**
     * @return The Visa Checkout API Key associated with this merchant's Visa Checkout configuration.
     */
    String getApiKey() {
        return apiKey;
    }

    /**
     * @return The accepted card brands for Visa Checkout.
     */
    List<String> getAcceptedCardBrands() {
        return cardBrands;
    }

    private static List<String> supportedCardTypesToAcceptedCardBrands(List<String> supportedCardTypes) {
        List<String> acceptedCardBrands = new ArrayList<>();

        for (String supportedCardType : supportedCardTypes) {
            switch (supportedCardType.toLowerCase(Locale.ROOT)) {
                case "visa":
                    acceptedCardBrands.add("VISA");
                    break;
                case "mastercard":
                    acceptedCardBrands.add("MASTERCARD");
                    break;
                case "discover":
                    acceptedCardBrands.add("DISCOVER");
                    break;
                case "american express":
                    acceptedCardBrands.add("AMEX");
                    break;
            }
        }

        return acceptedCardBrands;
    }
}
