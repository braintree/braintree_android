package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class GoogleWalletCard extends PaymentMethod implements Parcelable, Serializable {

    protected static final String PAYMENT_METHOD_TYPE = "GoogleWalletCard";

    @SerializedName("details")
    private GoogleWalletCardDetails mDetails;

    @Override
    public String getTypeLabel() {
        return "Google Wallet";
    }

    public String getLastTwo() {
        return mDetails.getLastTwo();
    }

    /**
     * Required for and handled by {@link com.braintreepayments.api.Braintree}. Not intended for general consumption.
     * @param googleWalletCard Raw JSON representation of a {@link com.braintreepayments.api.models.Card}.
     * @return {@link GoogleWalletCard} for use in payment method selection UIs.
     */
    public static GoogleWalletCard fromJson(String googleWalletCard) {
        return new Gson().fromJson(googleWalletCard, GoogleWalletCard.class);
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mDetails, 0);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeSerializable(mPaymentMethodOptions);
        dest.writeString(mSource);
    }

    public GoogleWalletCard() {}

    private GoogleWalletCard(Parcel in) {
        mDetails = in.readParcelable(GoogleWalletCardDetails.class.getClassLoader());
        mNonce = in.readString();
        mDescription = in.readString();
        mPaymentMethodOptions = (PaymentMethodOptions) in.readSerializable();
        mSource = in.readString();
    }

    public static final Creator<GoogleWalletCard> CREATOR = new Creator<GoogleWalletCard>() {
        public GoogleWalletCard createFromParcel(Parcel source) {
            return new GoogleWalletCard(source);
        }

        public GoogleWalletCard[] newArray(int size) {return new GoogleWalletCard[size];}
    };

    private static class GoogleWalletCardDetails implements Parcelable, Serializable {

        @SerializedName("cardType") private String mCardType;
        @SerializedName("lastTwo") private String mLastTwo;

        public GoogleWalletCardDetails() {}

        protected String getCardType() {
            return mCardType;
        }

        protected String getLastTwo() {
            return mLastTwo;
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mCardType);
            dest.writeString(mLastTwo);
        }

        private GoogleWalletCardDetails(Parcel in) {
            mCardType = in.readString();
            mLastTwo = in.readString();
        }

        public static final Creator<GoogleWalletCardDetails> CREATOR = new Creator<GoogleWalletCardDetails>() {
            public GoogleWalletCardDetails createFromParcel(Parcel source) {return new GoogleWalletCardDetails(source);}

            public GoogleWalletCardDetails[] newArray(int size) {return new GoogleWalletCardDetails[size];}
        };
    }
}
