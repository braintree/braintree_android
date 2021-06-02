package com.braintreepayments.api;

import android.os.Parcel;

import androidx.annotation.Nullable;

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
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";

    private static final String VENMO_DETAILS_KEY = "details";
    private static final String VENMO_USERNAME_KEY = "username";

    private static final String VENMO_PAYMENT_METHOD_ID_KEY = "paymentMethodId";
    private static final String VENMO_PAYMENT_METHOD_USERNAME_KEY = "userName";
    
    private final String username;

    static VenmoAccountNonce fromJSON(JSONObject inputJson) throws JSONException {
        JSONObject json;
        if (inputJson.has(API_RESOURCE_KEY)) {
            json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        } else {
            json = inputJson;
        }

        String nonce;
        boolean isDefault;
        String username;

        if (json.has(VENMO_PAYMENT_METHOD_ID_KEY)) {
            isDefault = false;
            nonce = json.getString(VENMO_PAYMENT_METHOD_ID_KEY);
            username = json.getString(VENMO_PAYMENT_METHOD_USERNAME_KEY);
        } else {
            nonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
            isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

            JSONObject details = json.getJSONObject(VENMO_DETAILS_KEY);
            username = details.getString(VENMO_USERNAME_KEY);
        }

        return new VenmoAccountNonce(nonce, username, isDefault);
    }

    VenmoAccountNonce(String nonce, String username, boolean isDefault) {
        super(nonce, isDefault);
        this.username = username;
    }

    /**
     * @return the Venmo username
     */
    @NonNull
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
