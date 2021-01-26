package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to parse and contain 3D Secure authentication responses
 */
public class ThreeDSecureAuthenticationResponse implements Parcelable {

    private static final String ERRORS_KEY = "errors";
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String PAYMENT_METHOD_KEY = "paymentMethod";
    private static final String SUCCESS_KEY = "success";

    private CardNonce mCardNonce;
    private boolean mSuccess;
    private String mErrors;
    private String mException;

    /**
     * Used to parse a response from the Braintree Gateway to be used for 3D Secure.
     *
     * @param jsonString The json response from the Braintree Gateway 3D Secure authentication route.
     * @return The {@link ThreeDSecureAuthenticationResponse} to use when performing 3D Secure
     * authentication.
     */
    public static ThreeDSecureAuthenticationResponse fromJson(String jsonString) {
        ThreeDSecureAuthenticationResponse authenticationResponse = new ThreeDSecureAuthenticationResponse();

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
                    authenticationResponse.mErrors = Json.optString(json.getJSONObject(ERROR_KEY), MESSAGE_KEY, null);
                }
                authenticationResponse.mSuccess = json.getBoolean(SUCCESS_KEY);
            } else {
                if (json.has(ERRORS_KEY)) {
                    authenticationResponse.mErrors = Json.optString(json.getJSONArray(ERRORS_KEY).getJSONObject(0), MESSAGE_KEY, null);
                }
                authenticationResponse.mSuccess = authenticationResponse.mErrors == null;
            }
        } catch (JSONException e) {
            authenticationResponse.mSuccess = false;
        }

        return authenticationResponse;
    }

    @Deprecated
    public static CardNonce getNonceWithAuthenticationDetails(String jsonString, CardNonce lookupCardNonce) {
        ThreeDSecureAuthenticationResponse authenticationResponse = new ThreeDSecureAuthenticationResponse();
        CardNonce nonceToReturn = lookupCardNonce;

        try {
            JSONObject json = new JSONObject(jsonString);

            if (json.has(SUCCESS_KEY)) {
                authenticationResponse.mSuccess = json.getBoolean(SUCCESS_KEY);
            } else if (!json.has(ERRORS_KEY)) {
                authenticationResponse.mSuccess = true;
            }

            if (authenticationResponse.mSuccess) {
                JSONObject cardJson = json.optJSONObject(PAYMENT_METHOD_KEY);
                if (cardJson != null) {
                    nonceToReturn = new CardNonce();
                    nonceToReturn.fromJson(cardJson);
                }
            } else {
                authenticationResponse.mErrors = jsonString;
            }
        } catch (JSONException e) {
            authenticationResponse.mSuccess = false;
            authenticationResponse.mException = e.getMessage();
        }

        nonceToReturn.getThreeDSecureInfo().setThreeDSecureAuthenticationResponse(authenticationResponse);
        return nonceToReturn;
    }

    /**
     * @deprecated ThreeDSecure WebView flow is deprecated. Use the browser-switch flow.
     * {@see https://developers.braintreepayments.com/guides/3d-secure/client-side/android/v2}
     */
    @Deprecated
    public static ThreeDSecureAuthenticationResponse fromException(String exception) {
        ThreeDSecureAuthenticationResponse authenticationResponse = new ThreeDSecureAuthenticationResponse();

        authenticationResponse.mSuccess = false;
        authenticationResponse.mException = exception;

        return authenticationResponse;
    }

    /**
     * @deprecated Use {@link ThreeDSecureInfo#isLiabilityShifted()} and
     * {@link ThreeDSecureInfo#isLiabilityShiftPossible()} to determine state of 3D Secure authentication
     * @return If the authentication was completed
     */
    @Deprecated
    public boolean isSuccess() {
        return mSuccess;
    }

    /**
     * @return The {@link CardNonce} associated with the 3D Secure
     * authentication
     */
    public CardNonce getCardNonce() {
        return mCardNonce;
    }

    /**
     * @deprecated Use {@link ThreeDSecureInfo#getErrorMessage()}
     * @return Possible errors that occurred during the authentication
     */
    @Deprecated
    public String getErrors() {
        // NEXT_MAJOR_VERSION make this a private method
        return mErrors;
    }

    /**
     * @return Possible exception that occurred during the authentication
     */
    public String getException() {
        return mException;
    }

    public ThreeDSecureAuthenticationResponse() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(mSuccess ? (byte) 1 : (byte) 0);
        dest.writeParcelable(mCardNonce, flags);
        dest.writeString(mErrors);
        dest.writeString(mException);
    }

    private ThreeDSecureAuthenticationResponse(Parcel in) {
        mSuccess = in.readByte() != 0;
        mCardNonce = in.readParcelable(CardNonce.class.getClassLoader());
        mErrors = in.readString();
        mException = in.readString();
    }

    public static final Creator<ThreeDSecureAuthenticationResponse> CREATOR =
            new Creator<ThreeDSecureAuthenticationResponse>() {
                public ThreeDSecureAuthenticationResponse createFromParcel(Parcel source) {
                    return new ThreeDSecureAuthenticationResponse(source);
                }

                public ThreeDSecureAuthenticationResponse[] newArray(int size) {
                    return new ThreeDSecureAuthenticationResponse[size];
                }
            };
}
