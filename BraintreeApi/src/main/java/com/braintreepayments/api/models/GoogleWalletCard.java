package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.Utils;
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
        return Utils.getGson().fromJson(googleWalletCard, GoogleWalletCard.class);
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mDetails, 0);
        dest.writeString(this.nonce);
        dest.writeString(this.description);
        dest.writeSerializable(this.options);
        dest.writeString(this.mSource);
    }

    public GoogleWalletCard() {}

    private GoogleWalletCard(Parcel in) {
        this.mDetails = in.readParcelable(GoogleWalletCardDetails.class.getClassLoader());
        this.nonce = in.readString();
        this.description = in.readString();
        this.options = (PaymentMethodOptions) in.readSerializable();
        this.mSource = in.readString();
    }

    public static final Creator<GoogleWalletCard> CREATOR = new Creator<GoogleWalletCard>() {
        public GoogleWalletCard createFromParcel(Parcel source) {
            return new GoogleWalletCard(source);
        }

        public GoogleWalletCard[] newArray(int size) {return new GoogleWalletCard[size];}
    };

    private static class GoogleWalletCardDetails implements Parcelable, Serializable {
        private String cardType;
        private String lastTwo;

        public GoogleWalletCardDetails() {}

        protected String getCardType() {
            return cardType;
        }

        protected String getLastTwo() {
            return lastTwo;
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.cardType);
            dest.writeString(this.lastTwo);
        }

        private GoogleWalletCardDetails(Parcel in) {
            this.cardType = in.readString();
            this.lastTwo = in.readString();
        }

        public static final Creator<GoogleWalletCardDetails> CREATOR = new Creator<GoogleWalletCardDetails>() {
            public GoogleWalletCardDetails createFromParcel(Parcel source) {return new GoogleWalletCardDetails(source);}

            public GoogleWalletCardDetails[] newArray(int size) {return new GoogleWalletCardDetails[size];}
        };
    }
}
