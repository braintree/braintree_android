package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *  Contains information about which payment methods are preferred on the device.
 *  This class is currently in beta and may change in future releases.
 */
public class PreferredPaymentMethodsResult {
    private boolean mPayPalPreferred;

    public PreferredPaymentMethodsResult() {}

    public static PreferredPaymentMethodsResult fromJSON(String responseBody) {
        boolean payPalPreferred = false;

        try {
            JSONObject response = new JSONObject(responseBody);
            JSONObject payPalClientConfiguration = getObjectAtKeyPath(response, "data.clientConfiguration.paypal");
            if (payPalClientConfiguration != null) {
                payPalPreferred = payPalClientConfiguration.getBoolean("preferredPaymentMethod");
            }
        } catch (JSONException ignored) {
            // do nothing
        }
        return new PreferredPaymentMethodsResult().isPayPalPreferred(payPalPreferred);
    }

    public PreferredPaymentMethodsResult isPayPalPreferred(boolean payPalPreferred) {
        mPayPalPreferred = payPalPreferred;
        return this;
    }

    /**
     *
     * @return True if PayPal is a preferred payment method. False otherwise.
     */
    public boolean isPayPalPreferred() {
        return mPayPalPreferred;
    }

    private static JSONObject getObjectAtKeyPath(JSONObject obj, String keyPath) throws JSONException {
        String[] keys = keyPath.split("\\.");
        JSONObject result = obj;
        for (String key : keys) {
            result = result.getJSONObject(key);
        }
        return result;
    }
}
