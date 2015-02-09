package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.annotations.Beta;
import com.braintreepayments.api.exceptions.ErrorWithResponse.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse.BraintreeErrors;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to parse and contain 3D Secure authentication responses
 */
@Beta
public class ThreeDSecureAuthenticationResponse implements Parcelable {

    private Card card;
    private boolean success;
    private BraintreeErrors errors;
    private String exception;

    public ThreeDSecureAuthenticationResponse() {}

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

            authenticationResponse.card = card;
            authenticationResponse.success = json.getBoolean("success");
        } catch (JSONException e) {
            authenticationResponse.success = false;
        }
        authenticationResponse.errors = gson.fromJson(jsonString, BraintreeErrors.class);

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
        authenticationResponse.success = false;
        authenticationResponse.exception = exception;

        return authenticationResponse;
    }

    /**
     * @return If the authentication was completed
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return The {@link com.braintreepayments.api.models.Card} associated with the 3D Secure
     *         authentication
     */
    public Card getCard() {
        return card;
    }

    /**
     * @return Possible errors that occurred during the authentication
     */
    public BraintreeErrors getErrors() {
        return errors;
    }

    /**
     * @return Possible exception that occurred during the authentication
     */
    public String getException() {
        return exception;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(success ? (byte) 1 : (byte) 0);
        dest.writeParcelable(card, flags);
        dest.writeParcelable(errors, flags);
        dest.writeString(exception);
    }

    private ThreeDSecureAuthenticationResponse(Parcel in) {
        success = in.readByte() != 0;
        card = in.readParcelable(Card.class.getClassLoader());
        errors = in.readParcelable(BraintreeError.class.getClassLoader());
        exception = in.readString();
    }

    public static final Creator<ThreeDSecureAuthenticationResponse> CREATOR =
            new Creator<ThreeDSecureAuthenticationResponse>() {
                public ThreeDSecureAuthenticationResponse createFromParcel(
                        Parcel source) {return new ThreeDSecureAuthenticationResponse(source);}

                public ThreeDSecureAuthenticationResponse[] newArray(
                        int size) {return new ThreeDSecureAuthenticationResponse[size];}
            };

}
