package com.braintreepayments.api;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * Contains the remote Kount configuration for the Braintree SDK.
 */
class KountConfiguration {

    private final static String KOUNT_MERCHANT_ID_KEY = "kountMerchantId";

    private String kountMerchantId;

    /**
     * Parse a {@link KountConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return a {@link KountConfiguration} instance with the data that was able to be parsed from
     * the {@link JSONObject}.
     */
    static KountConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        KountConfiguration kountConfiguration = new KountConfiguration();
        kountConfiguration.kountMerchantId = Json.optString(json, KOUNT_MERCHANT_ID_KEY, "");

        return kountConfiguration;
    }

    /**
     * @return {@code true} if Kount is enabled, {@code false} otherwise.
     */
    boolean isEnabled() {
        return !TextUtils.isEmpty(kountMerchantId);
    }

    /**
     * @return the Kount Merchant Id.
     */
    String getKountMerchantId() {
        return kountMerchantId;
    }
}
