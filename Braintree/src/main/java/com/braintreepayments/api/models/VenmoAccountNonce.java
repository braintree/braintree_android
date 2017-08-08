package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link PaymentMethodNonce} representing a {@link VenmoAccountNonce}
 * @see PaymentMethodNonce
 */
public class VenmoAccountNonce extends PaymentMethodNonce implements Parcelable {

    protected static final String TYPE = "VenmoAccount";
    protected static final String API_RESOURCE_KEY = "venmoAccounts";
    private static final String VENMO_DETAILS_KEY = "details";
    private static final String VENMO_USERNAME_KEY = "username";

    private String mUsername;

    public VenmoAccountNonce(String nonce, String description, String username) {
        mNonce = nonce;
        mDescription = description;
        mUsername = username;
    }

    /**
     * Convert an API response to an {@link VenmoAccountNonce}.
     *
     * @param json Raw JSON response from Braintree of a {@link VenmoAccountNonce}.
     * @return {@link VenmoAccountNonce}.
     * @throws JSONException when parsing the response fails.
     */
    public static VenmoAccountNonce fromJson(String json) throws JSONException {
        VenmoAccountNonce venmoAccountNonce = new VenmoAccountNonce();
        venmoAccountNonce.fromJson(VenmoAccountNonce.getJsonObjectForType(API_RESOURCE_KEY, json));
        return venmoAccountNonce;
    }

    protected void fromJson(JSONObject json) throws JSONException {
        super.fromJson(json);

        JSONObject details = json.getJSONObject(VENMO_DETAILS_KEY);
        mUsername = details.getString(VENMO_USERNAME_KEY);
        mDescription = mUsername;
    }

    /**
     * @return the Venmo username
     */
    public String getUsername() {
        return mUsername;
    }

    @Override
    public String getTypeLabel() {
        return "Venmo";
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mUsername);
    }

    public VenmoAccountNonce() {}

    protected VenmoAccountNonce(Parcel in) {
        super(in);
        mUsername = in.readString();
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
