package com.braintreepayments.api;


import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.braintreepayments.api.card.AuthenticationInsight;
import com.braintreepayments.api.card.BinData;
import com.braintreepayments.api.card.CardNonce;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link PaymentMethodNonce} representing a {@link CardNonce} that includes {@link ThreeDSecureInfo}
 */
public class ThreeDSecureNonce extends CardNonce {
    private static final String THREE_D_SECURE_INFO_KEY = "threeDSecureInfo";
    private final ThreeDSecureInfo threeDSecureInfo;

    private ThreeDSecureNonce(CardNonce cardNonce, ThreeDSecureInfo threeDSecureInfo) {
        this(threeDSecureInfo, cardNonce.getCardType(), cardNonce.getLastTwo(),
                cardNonce.getLastFour(), cardNonce.getBin(), cardNonce.getBinData(),
                cardNonce.getAuthenticationInsight(), cardNonce.getExpirationMonth(),
                cardNonce.getExpirationYear(), cardNonce.getCardholderName(), cardNonce.getString(),
                cardNonce.isDefault());
    }

    private ThreeDSecureNonce(ThreeDSecureInfo threeDSecureInfo, String cardType, String lastTwo,
                              String lastFour, String bin, BinData binData,
                              AuthenticationInsight authenticationInsight, String expirationMonth,
                              String expirationYear, String cardholderName, String nonce,
                              boolean isDefault) {
        super(cardType, lastTwo, lastFour, bin, binData, authenticationInsight, expirationMonth,
                expirationYear, cardholderName, nonce, isDefault);
        this.threeDSecureInfo = threeDSecureInfo;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static ThreeDSecureNonce fromJSON(JSONObject inputJson) throws JSONException {
        CardNonce cardNonce = CardNonce.fromJSON(inputJson);
        ThreeDSecureInfo threeDSecureInfo;
        if (inputJson.has(DATA_KEY)) { // graphQL
           threeDSecureInfo = ThreeDSecureInfo.fromJson(null);
        } else if (inputJson.has(API_RESOURCE_KEY)) { // REST
            JSONObject json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
            threeDSecureInfo =
                    ThreeDSecureInfo.fromJson(json.optJSONObject(THREE_D_SECURE_INFO_KEY));
        } else { // plain JSON
            threeDSecureInfo =
                    ThreeDSecureInfo.fromJson(inputJson.optJSONObject(THREE_D_SECURE_INFO_KEY));
        }
        return new ThreeDSecureNonce(cardNonce, threeDSecureInfo);
    }

    /**
     * @return The 3D Secure info for the current {@link ThreeDSecureNonce} or {@code null}
     */
    @NonNull
    public ThreeDSecureInfo getThreeDSecureInfo() {
        return threeDSecureInfo;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(threeDSecureInfo, flags);
    }

    protected ThreeDSecureNonce(Parcel in) {
        super(in);
        threeDSecureInfo = in.readParcelable(ThreeDSecureInfo.class.getClassLoader());
    }

    public static final Creator<ThreeDSecureNonce> CREATOR = new Creator<>() {
        public ThreeDSecureNonce createFromParcel(Parcel source) {
            return new ThreeDSecureNonce(source);
        }

        public ThreeDSecureNonce[] newArray(int size) {
            return new ThreeDSecureNonce[size];
        }
    };
}
