package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to parse and contain 3D Secure authentication responses
 */
public class ThreeDSecureResult implements Parcelable {

    private static final String ERRORS_KEY = "errors";
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String PAYMENT_METHOD_KEY = "paymentMethod";
    private static final String LOOKUP_KEY = "lookup";

    private CardNonce mCardNonce;
    private String mErrorMessage;

    private ThreeDSecureLookup mLookup;

    /**
     * Used to parse a response from the Braintree Gateway to be used for 3D Secure.
     *
     * @param jsonString The json response from the Braintree Gateway 3D Secure authentication route.
     * @return The {@link ThreeDSecureResult} to use when performing 3D Secure
     * authentication.
     */
    static ThreeDSecureResult fromJson(String jsonString) throws JSONException {
        ThreeDSecureResult result = new ThreeDSecureResult();
        JSONObject json = new JSONObject(jsonString);

        JSONObject cardJson = json.optJSONObject(PAYMENT_METHOD_KEY);
        if (cardJson != null) {
            CardNonce cardNonce = new CardNonce();
            cardNonce.fromJson(cardJson);
            result.mCardNonce = cardNonce;
        }

        if (json.has(ERRORS_KEY)) {
            // 3DS v2
            result.mErrorMessage = Json.optString(json.getJSONArray(ERRORS_KEY).getJSONObject(0), MESSAGE_KEY, null);
        } else if (json.has(ERROR_KEY)) {
            // 3DS v1
            result.mErrorMessage = Json.optString(json.getJSONObject(ERROR_KEY), MESSAGE_KEY, null);
        }

        if (json.has(LOOKUP_KEY)) {
            String lookupJson = json.getJSONObject(LOOKUP_KEY).toString();
            result.mLookup = ThreeDSecureLookup.fromJson(lookupJson);
        }

        return result;
    }

    /**
     * @return The {@link CardNonce} associated with the 3D Secure
     * authentication
     */
    public CardNonce getCardNonce() {
        return mCardNonce;
    }

    void setCardNonce(CardNonce cardNonce) {
        mCardNonce = cardNonce;
    }

    /**
     * @return Message describing potential errors that occurred during the authentication
     */
    public String getErrorMessage() {
        return mErrorMessage;
    }

    boolean hasError() {
        return (mErrorMessage != null && mErrorMessage.length() > 0);
    }

    ThreeDSecureLookup getLookup() {
        return mLookup;
    }

    ThreeDSecureResult() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mCardNonce, flags);
        dest.writeString(mErrorMessage);
        dest.writeParcelable(mLookup, flags);
    }

    private ThreeDSecureResult(Parcel in) {
        mCardNonce = in.readParcelable(CardNonce.class.getClassLoader());
        mErrorMessage = in.readString();
        mLookup = in.readParcelable(ThreeDSecureLookup.class.getClassLoader());
    }

    public static final Creator<ThreeDSecureResult> CREATOR =
            new Creator<ThreeDSecureResult>() {
                public ThreeDSecureResult createFromParcel(Parcel source) {
                    return new ThreeDSecureResult(source);
                }

                public ThreeDSecureResult[] newArray(int size) {
                    return new ThreeDSecureResult[size];
                }
            };
}
