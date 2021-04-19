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

    static final String TYPE = "VenmoAccount";
    private static final String API_RESOURCE_KEY = "venmoAccounts";

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";

    private static final String VENMO_DETAILS_KEY = "details";
    private static final String VENMO_USERNAME_KEY = "username";

    private final String mUsername;

    private final String mNonce;
    private final String mDescription;
    private final boolean mDefault;

    static VenmoAccountNonce fromJSON(JSONObject inputJson) throws JSONException {
        JSONObject json;
        if (inputJson.has(API_RESOURCE_KEY)) {
            json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        } else {
            json = inputJson;
        }

        String nonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        boolean isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

        JSONObject details = json.getJSONObject(VENMO_DETAILS_KEY);
        String username = details.getString(VENMO_USERNAME_KEY);

        return new VenmoAccountNonce(nonce, username, isDefault);
    }

    VenmoAccountNonce(String nonce, String username, boolean isDefault) {
        super(nonce, username, isDefault, "TODO", PaymentMethodType.VENMO);
        mNonce = nonce;
        mUsername = username;
        mDescription = username;
        mDefault = isDefault;
    }

    /**
     * @return the Venmo username
     */
    public String getUsername() {
        return mUsername;
    }

    @Override
    public String getString() {
        return mNonce;
    }

    @Override
    public String getDescription() {
        return mDescription;
    }

    @Override
    public boolean isDefault() {
        return mDefault;
    }

    @Override
    public String getTypeLabel() {
        return "Venmo";
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mUsername);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeByte(mDefault ? (byte) 1 : (byte) 0);
    }

    private VenmoAccountNonce(Parcel in) {
        super(in);
        mUsername = in.readString();
        mNonce = in.readString();
        mDescription = in.readString();
        mDefault = in.readByte() > 0;
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
