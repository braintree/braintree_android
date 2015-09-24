package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.exceptions.InvalidArgumentException;

/**
 * Generic base class for Braintree authorization
 */
public abstract class Authorization implements Parcelable {

    private final String mRawValue;

    public Authorization(String rawValue) {
        mRawValue = rawValue;
    }

    /**
     * Returns an {@link Authorization} of the correct type for a given {@link String}.
     *
     * @param authorizationString Given string to transform into an {@link Authorization}.
     * @return {@link Authorization}
     * @throws InvalidArgumentException This method will throw this exception type if the string
     * passed does not meet any of the criteria supplied for {@link ClientToken} or {@link ClientKey}.
     */
    public static Authorization fromString(String authorizationString) throws InvalidArgumentException {
        if (isClientKey(authorizationString)) {
            return new ClientKey(authorizationString);
        } else {
            return new ClientToken(authorizationString);
        }
    }

    /**
     * @return The url to fetch configuration for the current Braintree environment.
     */
    public abstract String getConfigUrl();

    /**
     * @return The original Client token or Client key string, which can be used for serialization
     */
    @Override
    public String toString() {
        return mRawValue;
    }

    private static boolean isClientKey(String blob) {
        return blob.matches(ClientKey.MATCHER);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mRawValue);
    }

    public Authorization(Parcel in) {
        mRawValue = in.readString();
    }
}
