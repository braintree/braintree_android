package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains information about which payment methods are preferred on the device.
 * This class is currently in beta and may change in future releases.
 */
public class PreferredPaymentMethodsResult {
    private boolean mPayPalPreferred;
    private boolean mVenmoPreferred;

    PreferredPaymentMethodsResult() {
    }

    static PreferredPaymentMethodsResult fromJSON(String responseBody, boolean venmoInstalled) {
        boolean payPalPreferred = false;

        try {
            JSONObject response = new JSONObject(responseBody);
            JSONObject preferredPaymentMethods = getObjectAtKeyPath(response, "data.preferredPaymentMethods");
            if (preferredPaymentMethods != null) {
                payPalPreferred = preferredPaymentMethods.getBoolean("paypalPreferred");
            }
        } catch (JSONException ignored) {
            // do nothing
        }

        PreferredPaymentMethodsResult result = new PreferredPaymentMethodsResult();
        result.isPayPalPreferred(payPalPreferred);
        result.isVenmoPreferred(venmoInstalled);
        return result;
    }

    public void isPayPalPreferred(boolean payPalPreferred) {
        mPayPalPreferred = payPalPreferred;
    }

    public void isVenmoPreferred(boolean venmoPreferred) {
        mVenmoPreferred = venmoPreferred;
    }

    /**
     * @return True if PayPal is a preferred payment method. False otherwise.
     */
    public boolean isPayPalPreferred() {
        return mPayPalPreferred;
    }

    /**
     * @return True if Venmo app is installed. False otherwise.
     */
    public boolean isVenmoPreferred() {
        return mVenmoPreferred;
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
