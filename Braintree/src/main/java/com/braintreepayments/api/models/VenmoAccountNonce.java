package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * {@link PaymentMethodNonce} representing a {@link VenmoAccountNonce}
 * @see PaymentMethodNonce
 */
public class VenmoAccountNonce extends PaymentMethodNonce implements Parcelable {

    private static final String TYPE = "Venmo";

    private String mUsername;

    public VenmoAccountNonce(String nonce, String description, String username) {
        mNonce = nonce;
        mDescription = description;
        mUsername = username;
    }

    /**
     * @return the Venmo username
     */
    public String getUsername() {
        return mUsername;
    }

    @Override
    public String getTypeLabel() {
        return TYPE;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mUsername);
    }

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
