package com.braintreepayments.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Contains the remote card configuration for the Braintree SDK.
 */
class CardConfiguration {

    private static final String SUPPORTED_CARD_TYPES_KEY = "supportedCardTypes";
    private static final String COLLECT_DEVICE_DATA_KEY = "collectDeviceData";

    private final List<String> supportedCardTypes = new ArrayList<>();
    private boolean collectFraudData = false;

    /**
     * Parse a {@link CardConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return a {@link CardConfiguration} instance with the data that was able to be parsed from the {@link JSONObject}.
     */
    static CardConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        CardConfiguration cardConfiguration = new CardConfiguration();

        JSONArray jsonArray = json.optJSONArray(SUPPORTED_CARD_TYPES_KEY);
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                cardConfiguration.supportedCardTypes.add(jsonArray.optString(i, ""));
            }
        }
        cardConfiguration.collectFraudData = json.optBoolean(COLLECT_DEVICE_DATA_KEY, false);
        return cardConfiguration;
    }

    /**
     * @return a {@link Set<String>} of card types supported by the merchant.
     */
    List<String> getSupportedCardTypes() {
        return Collections.unmodifiableList(supportedCardTypes);
    }

    /**
     * @return if fraud data collection should occur.
     */
    boolean isFraudDataCollectionEnabled() {
        return collectFraudData;
    }
}
