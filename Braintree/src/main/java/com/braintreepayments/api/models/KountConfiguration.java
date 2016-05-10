package com.braintreepayments.api.models;

import org.json.JSONObject;

/**
 * Contains the remote Kount configuration for the Braintree SDK.
 */
public class KountConfiguration {

    private final static String ENABLED_KEY = "enabled";
    private final static String KOUNT_MERCHANT_ID_KEY = "kountMerchantId";

    private boolean mEnabled;
    private String mKountMerchantId;

    /**
     * Parse a {@link KountConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return a {@link KountConfiguration} instance with the data that was able to be parsed from
     * the {@link JSONObject}.
     */
    public static KountConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        KountConfiguration kountConfiguration = new KountConfiguration();
        kountConfiguration.mEnabled = json.optBoolean(ENABLED_KEY);
        kountConfiguration.mKountMerchantId = json.optString(KOUNT_MERCHANT_ID_KEY, "0");

        return kountConfiguration;
    }

    /**
     * @return {@code true} if Kount is enabled, {@code false} otherwise.
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * @return the Kount Merchant Id.
     */
    public String getKountMerchantId() {
        return mKountMerchantId;
    }
}
