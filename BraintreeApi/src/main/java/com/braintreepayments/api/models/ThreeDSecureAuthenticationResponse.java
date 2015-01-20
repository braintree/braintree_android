package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.Utils;
import com.braintreepayments.api.exceptions.ErrorWithResponse.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse.BraintreeErrors;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to parse and contain 3D Secure authentication responses
 */
public class ThreeDSecureAuthenticationResponse implements Parcelable {

    private Card card;
    private boolean success;
    private BraintreeErrors errors;

    public ThreeDSecureAuthenticationResponse() {}

    public static ThreeDSecureAuthenticationResponse fromJson(String jsonString) {
        ThreeDSecureAuthenticationResponse authenticationResponse = new ThreeDSecureAuthenticationResponse();

        try {
            JSONObject json = new JSONObject(jsonString);

            Card card = Utils.getGson()
                    .fromJson(json.getJSONObject("paymentMethod").toString(), Card.class);
            card.setThreeDSecureInfo(Utils.getGson().fromJson(
                    json.getJSONObject("threeDSecureInfo").toString(), ThreeDSecureInfo.class));

            authenticationResponse.card = card;
            authenticationResponse.success = json.getBoolean("success");
        } catch (JSONException e) {
            authenticationResponse.success = false;
        }
        authenticationResponse.errors =
                Utils.getGson().fromJson(jsonString, BraintreeErrors.class);

        return authenticationResponse;
    }

    public boolean isSuccess() {
        return success;
    }

    public Card getCard() {
        return card;
    }

    public BraintreeErrors getErrors() {
        return errors;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(success ? (byte) 1 : (byte) 0);
        dest.writeParcelable(card, flags);
        dest.writeParcelable(errors, flags);
    }

    private ThreeDSecureAuthenticationResponse(Parcel in) {
        success = in.readByte() != 0;
        card = in.readParcelable(Card.class.getClassLoader());
        errors = in.readParcelable(BraintreeError.class.getClassLoader());
    }

    public static final Creator<ThreeDSecureAuthenticationResponse> CREATOR =
            new Creator<ThreeDSecureAuthenticationResponse>() {
                public ThreeDSecureAuthenticationResponse createFromParcel(
                        Parcel source) {return new ThreeDSecureAuthenticationResponse(source);}

                public ThreeDSecureAuthenticationResponse[] newArray(
                        int size) {return new ThreeDSecureAuthenticationResponse[size];}
            };

}
