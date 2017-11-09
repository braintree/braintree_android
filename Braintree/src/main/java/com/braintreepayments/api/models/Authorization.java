package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

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
     * passed does not meet any of the criteria supplied for {@link ClientToken} or {@link TokenizationKey}.
     */
    public static Authorization fromString(@Nullable String authorizationString) throws InvalidArgumentException {
        if (isTokenizationKey(authorizationString)) {
            return new TokenizationKey(authorizationString);
        } else {
            return new ClientToken(authorizationString);
        }
    }

    /**
     * @return The url to fetch configuration for the current Braintree environment.
     */
    public abstract String getConfigUrl();

    /**
     * @return The authorization string for authorizing requests.
     */
    public abstract String getAuthorization();

    /**
     * @return The original Client token or Tokenization Key string, which can be used for serialization
     */
    @Override
    public String toString() {
        return mRawValue;
    }

    /**
     * @param tokenizationKey The {@link String} to check if it is a tokenization key
     * @return {@code true} if the {@link String} is a tokenization key, {@code false} otherwise.
     */
    public static boolean isTokenizationKey(String tokenizationKey) {
        return !TextUtils.isEmpty(tokenizationKey) && tokenizationKey.matches(TokenizationKey.MATCHER);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mRawValue);
    }

    public Authorization(Parcel in) {
        mRawValue = in.readString();
    }
}
