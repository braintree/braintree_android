package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link BraintreeNonce} representing a {@link VenmoAccountNonce}
 *
 * @see BraintreeNonce
 */
public class VenmoAccountNonce implements PaymentMethodNonce {

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";
    private static final String DESCRIPTION_KEY = "description";

    static final String TYPE = "VenmoAccount";
    static final String API_RESOURCE_KEY = "venmoAccounts";
    private static final String VENMO_DETAILS_KEY = "details";
    private static final String VENMO_USERNAME_KEY = "username";

    private String mUsername;

    protected String mNonce;
    protected String mDescription;
    protected boolean mDefault;

    VenmoAccountNonce(String nonce, String description, String username) {
        mNonce = nonce;
        mDescription = description;
        mUsername = username;
    }

    VenmoAccountNonce(String jsonString) throws JSONException {
        this(new JSONObject(jsonString));
    }

    VenmoAccountNonce(JSONObject inputJson) throws JSONException {

        JSONObject json;
        if (inputJson.has(API_RESOURCE_KEY)) {
            json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        } else {
            json = inputJson;
        }

        mNonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        mDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

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
    public String getNonce() {
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
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mUsername);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeByte(mDefault ? (byte) 1 : (byte) 0);
    }

    private VenmoAccountNonce(Parcel in) {
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
