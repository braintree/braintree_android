package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link PaymentMethodNonce} representing a {@link VenmoAccountNonce}
 *
 * @see PaymentMethodNonce
 */
public class VenmoAccountNonce extends PaymentMethodNonce {

    private static final String API_RESOURCE_KEY = "venmoAccounts";
    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";

    private static final String VENMO_DETAILS_KEY = "details";
    private static final String VENMO_USERNAME_KEY = "username";

    private final String username;

    static VenmoAccountNonce fromJSON(JSONObject inputJson) throws JSONException {
        JSONObject json;
        if (inputJson.has(API_RESOURCE_KEY)) {
            json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        } else {
            json = inputJson;
        }

        String nonce = json.getString(PAYMENT_METHOD_NONCE_KEY);

        JSONObject details = json.getJSONObject(VENMO_DETAILS_KEY);
        String username = details.getString(VENMO_USERNAME_KEY);

        return new VenmoAccountNonce(nonce, username);
    }

    VenmoAccountNonce(String nonce, String username) {
        super(nonce, PaymentMethodType.VENMO, "Venmo", username);
        this.username = username;
    }

    /**
     * @return the Venmo username
     */
    public String getUsername() {
        return username;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(username);
    }

    private VenmoAccountNonce(Parcel in) {
        super(in);
        username = in.readString();
    }

    public static final Creator<VenmoAccountNonce> CREATOR = new Creator<VenmoAccountNonce>() {
        @Override
        public VenmoAccountNonce createFromParcel(Parcel in) {
            return new VenmoAccountNonce(in);
        }

        @Override
        public VenmoAccountNonce[] newArray(int size) {
            return new VenmoAccountNonce[size];
        }
    };
}
