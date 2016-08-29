package com.braintreepayments.api.models;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Contains the remote card configuration for the Braintree SDK.
 */
public class CardConfiguration {

    private static final String SUPPORTED_CARD_TYPES_KEY = "supportedCardTypes";

    private String[] mSupportedCardTypes;

    /**
     * Parse a {@link CardConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return a {@link CardConfiguration} instance with the data that was able to be parsed from the {@link JSONObject}.
     */
    public static CardConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        CardConfiguration cardConfiguration = new CardConfiguration();

        JSONArray jsonArray = json.optJSONArray(SUPPORTED_CARD_TYPES_KEY);
        if (jsonArray == null) {
            cardConfiguration.mSupportedCardTypes = new String[0];
        } else {
            cardConfiguration.mSupportedCardTypes = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                cardConfiguration.mSupportedCardTypes[i] = jsonArray.optString(i, "");
            }
        }

        return cardConfiguration;
    }

    /**
     * @return a {@link String} array of card types supported by the merchant.
     */
    public String[] getSupportedCardTypes() {
        return mSupportedCardTypes.clone();
    }
}
