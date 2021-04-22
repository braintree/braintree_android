package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

/**
 * Generic base class for Braintree authorization
 */
abstract class Authorization implements Parcelable {

    private final String rawValue;

    Authorization(String rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * Returns an {@link Authorization} of the correct type for a given {@link String}.
     *
     * @param authorizationString Given string to transform into an {@link Authorization}.
     * @return {@link Authorization}
     * @throws InvalidArgumentException This method will throw this exception type if the string
     * passed does not meet any of the criteria supplied for {@link ClientToken} or {@link TokenizationKey}.
     */
    static Authorization fromString(@Nullable String authorizationString) throws InvalidArgumentException {
        if (isTokenizationKey(authorizationString)) {
            return new TokenizationKey(authorizationString);
        } else if (isPayPalUAT(authorizationString)){
            return new PayPalUAT(authorizationString);
        } else if (isClientToken(authorizationString)) {
            return new ClientToken(authorizationString);
        } else {
            throw new InvalidArgumentException("Authorization provided is invalid: " + authorizationString);
        }
    }

    /**
     * @return The url to fetch configuration for the current Braintree environment.
     */
    abstract String getConfigUrl();

    /**
     * @return The authorization bearer string for authorizing requests.
     */
    abstract String getBearer();

    /**
     * @return The original Client token or Tokenization Key string, which can be used for serialization
     */
    @Override
    public String toString() {
        return rawValue;
    }

    private static boolean isTokenizationKey(String tokenizationKey) {
        return !TextUtils.isEmpty(tokenizationKey) && tokenizationKey.matches(TokenizationKey.MATCHER);
    }

    private static boolean isPayPalUAT(String payPalUAT) {
        return !TextUtils.isEmpty(payPalUAT) && payPalUAT.matches(PayPalUAT.MATCHER);
    }

    private static boolean isClientToken(String clientToken) {
        return !TextUtils.isEmpty(clientToken) && clientToken.matches(ClientToken.BASE_64_MATCHER);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(rawValue);
    }

    public Authorization(Parcel in) {
        rawValue = in.readString();
    }
}
