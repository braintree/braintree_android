package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to parse and contain 3D Secure authentication responses
 */
// TODO: Split into separate result objects for createPaymentAuthRequest and tokenize methods
public class ThreeDSecureResult implements Parcelable {

    private static final String ERRORS_KEY = "errors";
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String PAYMENT_METHOD_KEY = "paymentMethod";
    private static final String LOOKUP_KEY = "lookup";

    private ThreeDSecureNonce threeDSecureNonce;
    private String errorMessage;

    private ThreeDSecureLookup lookup;

    /**
     * Used to parse a response from the Braintree Gateway to be used for 3D Secure.
     *
     * @param jsonString The json response from the Braintree Gateway 3D Secure authentication
     *                   route.
     * @return The {@link ThreeDSecureResult} to use when performing 3D Secure authentication.
     */
    static ThreeDSecureResult fromJson(String jsonString) throws JSONException {
        ThreeDSecureResult result = new ThreeDSecureResult();
        JSONObject json = new JSONObject(jsonString);

        JSONObject cardJson = json.optJSONObject(PAYMENT_METHOD_KEY);
        if (cardJson != null) {
            result.threeDSecureNonce = ThreeDSecureNonce.fromJSON(cardJson);
        }

        if (json.has(ERRORS_KEY)) {
            // 3DS v2
            result.errorMessage =
                    Json.optString(json.getJSONArray(ERRORS_KEY).getJSONObject(0), MESSAGE_KEY,
                            null);
        } else if (json.has(ERROR_KEY)) {
            // 3DS v1
            result.errorMessage = Json.optString(json.getJSONObject(ERROR_KEY), MESSAGE_KEY, null);
        }

        if (json.has(LOOKUP_KEY)) {
            String lookupJson = json.getJSONObject(LOOKUP_KEY).toString();
            result.lookup = ThreeDSecureLookup.fromJson(lookupJson);
        }

        return result;
    }

    /**
     * @return The {@link ThreeDSecureNonce} associated with the 3D Secure authentication
     */
    @Nullable
    public ThreeDSecureNonce getThreeDSecureNonce() {
        return threeDSecureNonce;
    }

    void setThreeDSecureNonce(@Nullable ThreeDSecureNonce cardNonce) {
        threeDSecureNonce = cardNonce;
    }

    /**
     * @return Message describing potential errors that occurred during the authentication
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    boolean hasError() {
        return (errorMessage != null && errorMessage.length() > 0);
    }

    /**
     * @return {@link ThreeDSecureLookup} containing details of the 3D Secure lookup.
     */
    public ThreeDSecureLookup getLookup() {
        return lookup;
    }

    ThreeDSecureResult() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(threeDSecureNonce, flags);
        dest.writeString(errorMessage);
        dest.writeParcelable(lookup, flags);
    }

    private ThreeDSecureResult(Parcel in) {
        threeDSecureNonce = in.readParcelable(CardNonce.class.getClassLoader());
        errorMessage = in.readString();
        lookup = in.readParcelable(ThreeDSecureLookup.class.getClassLoader());
    }

    public static final Creator<ThreeDSecureResult> CREATOR = new Creator<>() {
        public ThreeDSecureResult createFromParcel(Parcel source) {
            return new ThreeDSecureResult(source);
        }

        public ThreeDSecureResult[] newArray(int size) {
            return new ThreeDSecureResult[size];
        }
    };
}
