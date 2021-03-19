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
    private static final String SUCCESS_KEY = "success";
    private static final String LOOKUP_KEY = "lookup";

    private CardNonce mCardNonce;
    private boolean mSuccess;
    private String mErrorMessage;
    private String mException;

    private ThreeDSecureLookup mLookup;

    /**
     * Used to parse a response from the Braintree Gateway to be used for 3D Secure.
     *
     * @param jsonString The json response from the Braintree Gateway 3D Secure authentication route.
     * @return The {@link ThreeDSecureResult} to use when performing 3D Secure
     * authentication.
     */
    static ThreeDSecureResult fromJson(String jsonString) {
        ThreeDSecureResult authenticationResponse = new ThreeDSecureResult();

        try {
            JSONObject json = new JSONObject(jsonString);

            JSONObject cardJson = json.optJSONObject(PAYMENT_METHOD_KEY);
            if (cardJson != null) {
                CardNonce cardNonce = new CardNonce();
                cardNonce.fromJson(cardJson);
                authenticationResponse.mCardNonce = cardNonce;
            }

            // 3DS 1.0 has a "success" key, but 3DS 2.0 responses do not.
            if (json.has(SUCCESS_KEY)) {
                if (json.has(ERROR_KEY)) {
                    authenticationResponse.mErrorMessage = Json.optString(json.getJSONObject(ERROR_KEY), MESSAGE_KEY, null);
                }
                authenticationResponse.mSuccess = json.getBoolean(SUCCESS_KEY);
            } else {
                if (json.has(ERRORS_KEY)) {
                    authenticationResponse.mErrorMessage = Json.optString(json.getJSONArray(ERRORS_KEY).getJSONObject(0), MESSAGE_KEY, null);
                }
                authenticationResponse.mSuccess = authenticationResponse.mErrorMessage == null;
            }

            if (json.has(LOOKUP_KEY)) {
                String lookupJson = json.getJSONObject(LOOKUP_KEY).toString();
                authenticationResponse.mLookup = ThreeDSecureLookup.fromJson(lookupJson);
            }

        } catch (JSONException e) {
            authenticationResponse.mSuccess = false;
        }

        return authenticationResponse;
    }

    /**
     * @deprecated ThreeDSecure WebView flow is deprecated. Use the browser-switch flow.
     * {@see https://developers.braintreepayments.com/guides/3d-secure/client-side/android/v2}
     */
    @Deprecated
    public static ThreeDSecureResult fromException(String exception) {
        ThreeDSecureResult authenticationResponse = new ThreeDSecureResult();

        authenticationResponse.mSuccess = false;
        authenticationResponse.mException = exception;

        return authenticationResponse;
    }

    /**
     * @return If the authentication was completed
     */
    boolean isSuccess() {
        return mSuccess;
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

    /**
     * @return Possible exception that occurred during the authentication
     */
    public String getException() {
        return mException;
    }

    ThreeDSecureResult() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(mSuccess ? (byte) 1 : (byte) 0);
        dest.writeParcelable(mCardNonce, flags);
        dest.writeString(mErrorMessage);
        dest.writeString(mException);
        dest.writeParcelable(mLookup, flags);
    }

    private ThreeDSecureResult(Parcel in) {
        mSuccess = in.readByte() != 0;
        mCardNonce = in.readParcelable(CardNonce.class.getClassLoader());
        mErrorMessage = in.readString();
        mException = in.readString();
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
