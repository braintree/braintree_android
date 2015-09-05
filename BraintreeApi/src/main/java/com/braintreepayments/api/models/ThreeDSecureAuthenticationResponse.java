package com.braintreepayments.api.models;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.annotations.Beta;
import com.braintreepayments.api.exceptions.ErrorWithResponse.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse.BraintreeErrors;
import com.braintreepayments.api.threedsecure.ThreeDSecureWebViewActivity;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to parse and contain 3D Secure authentication responses
 */
@Beta
public class ThreeDSecureAuthenticationResponse implements Parcelable {

    @SerializedName("card") private Card mCard;
    @SerializedName("success") private boolean mSuccess;
    @SerializedName("errors") private BraintreeErrors mErrors;
    @SerializedName("exception") private String mException;

    public ThreeDSecureAuthenticationResponse() {}

    /**
     * Checks the given {@link Intent} to see if it contains a {@link ThreeDSecureAuthenticationResponse}.
     *
     * @param intent The {@link Intent} to check.
     * @return {@code true} is the {@link Intent} contains a {@link ThreeDSecureWebViewActivity#EXTRA_THREE_D_SECURE_RESULT},
     *         {@code false} otherwise.
     */
    public static boolean isThreeDSecureAuthenticationResponse(Intent intent) {
        return intent.hasExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT);
    }

    /**
     * Used to parse a response from the Braintree Gateway to be used for 3D Secure
     *
     * @param jsonString The json response from the Braintree Gateway 3D Secure authentication route
     * @return The {@link com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse} to use when
     *         performing 3D Secure authentication
     */
    public static ThreeDSecureAuthenticationResponse fromJson(String jsonString) {
        ThreeDSecureAuthenticationResponse authenticationResponse = new ThreeDSecureAuthenticationResponse();
        Gson gson = new Gson();

        try {
            JSONObject json = new JSONObject(jsonString);

            Card card = gson.fromJson(json.getJSONObject("paymentMethod").toString(), Card.class);
            card.setThreeDSecureInfo(gson.fromJson(json.getJSONObject("threeDSecureInfo").toString(),
                    ThreeDSecureInfo.class));

            authenticationResponse.mCard = card;
            authenticationResponse.mSuccess = json.getBoolean("success");
        } catch (JSONException e) {
            authenticationResponse.mSuccess = false;
        }
        authenticationResponse.mErrors = gson.fromJson(jsonString, BraintreeErrors.class);

        return authenticationResponse;
    }

    /**
     * Used to handle exceptions that occur during 3D Secure authentication
     *
     * @param exception The message of the exception that occured
     * @return The {@link com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse} to be
     *         handled by {@link com.braintreepayments.api.Braintree}
     */
    public static ThreeDSecureAuthenticationResponse fromException(String exception) {
        ThreeDSecureAuthenticationResponse authenticationResponse = new ThreeDSecureAuthenticationResponse();
        authenticationResponse.mSuccess = false;
        authenticationResponse.mException = exception;

        return authenticationResponse;
    }

    /**
     * @return If the authentication was completed
     */
    public boolean isSuccess() {
        return mSuccess;
    }

    /**
     * @return The {@link com.braintreepayments.api.models.Card} associated with the 3D Secure
     *         authentication
     */
    public Card getCard() {
        return mCard;
    }

    /**
     * @return Possible errors that occurred during the authentication
     */
    public BraintreeErrors getErrors() {
        return mErrors;
    }

    /**
     * @return Possible exception that occurred during the authentication
     */
    public String getException() {
        return mException;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(mSuccess ? (byte) 1 : (byte) 0);
        dest.writeParcelable(mCard, flags);
        dest.writeParcelable(mErrors, flags);
        dest.writeString(mException);
    }

    private ThreeDSecureAuthenticationResponse(Parcel in) {
        mSuccess = in.readByte() != 0;
        mCard = in.readParcelable(Card.class.getClassLoader());
        mErrors = in.readParcelable(BraintreeError.class.getClassLoader());
        mException = in.readString();
    }

    public static final Creator<ThreeDSecureAuthenticationResponse> CREATOR =
            new Creator<ThreeDSecureAuthenticationResponse>() {
                public ThreeDSecureAuthenticationResponse createFromParcel(
                        Parcel source) {return new ThreeDSecureAuthenticationResponse(source);}

                public ThreeDSecureAuthenticationResponse[] newArray(
                        int size) {return new ThreeDSecureAuthenticationResponse[size];}
            };

}
