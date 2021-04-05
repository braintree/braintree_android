package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link PaymentMethodNonce} representing a {@link VenmoAccountNonce}
 *
 * @see PaymentMethodNonce
 */
public class VenmoAccountNonce extends PaymentMethodNonce implements Parcelable {

    static final String TYPE = "VenmoAccount";
    static final String API_RESOURCE_KEY = "venmoAccounts";
    private static final String VENMO_DETAILS_KEY = "details";
    private static final String VENMO_USERNAME_KEY = "username";

    private String mUsername;

    VenmoAccountNonce(String nonce, String description, String username) {
        // TODO: consider creating JSON object here and calling JSON constructor
        mNonce = nonce;
        mDescription = description;
        mUsername = username;
    }

    VenmoAccountNonce(String jsonString) throws JSONException {
        super(jsonString);
    }

    VenmoAccountNonce(JSONObject inputJson) throws JSONException {
        super(inputJson);

        JSONObject json;
        if (inputJson.has(VenmoAccountNonce.API_RESOURCE_KEY)) {
            json = VenmoAccountNonce.getJsonObjectForType(API_RESOURCE_KEY, inputJson);
        } else {
            json = inputJson;
        }

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

    VenmoAccountNonce() {
    }

    private VenmoAccountNonce(Parcel in) {
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
