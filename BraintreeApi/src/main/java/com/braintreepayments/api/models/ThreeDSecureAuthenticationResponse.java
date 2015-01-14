package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.Utils;
import com.braintreepayments.api.exceptions.ErrorWithResponse.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse.BraintreeErrors;
import com.braintreepayments.api.exceptions.ThreeDSecureInfo;

/**
 * Class to parse and contain 3D Secure authentication responses
 */
public class ThreeDSecureAuthenticationResponse implements Parcelable {

    private boolean success;
    private Card paymentMethod;
    private ThreeDSecureInfo threeDSecureInfo;
    private BraintreeErrors errors;

    public ThreeDSecureAuthenticationResponse() {}

    public static ThreeDSecureAuthenticationResponse fromJson(String jsonString) {
        ThreeDSecureAuthenticationResponse authenticationResponse =
                Utils.getGson().fromJson(jsonString, ThreeDSecureAuthenticationResponse.class);
        authenticationResponse.errors = Utils.getGson().fromJson(jsonString, BraintreeErrors.class);

        return authenticationResponse;
    }

    public boolean isSuccess() {
        return success;
    }

    public Card getCard() {
        return paymentMethod;
    }

    public ThreeDSecureInfo getThreeDSecureInfo() {
        return threeDSecureInfo;
    }

    public BraintreeErrors getErrors() {
        return errors;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(success ? (byte) 1 : (byte) 0);
        dest.writeParcelable(paymentMethod, flags);
        dest.writeParcelable(threeDSecureInfo, flags);
        dest.writeParcelable(errors, flags);
    }

    private ThreeDSecureAuthenticationResponse(Parcel in) {
        success = in.readByte() != 0;
        paymentMethod = in.readParcelable(Card.class.getClassLoader());
        threeDSecureInfo = in.readParcelable(ThreeDSecureInfo.class.getClassLoader());
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
