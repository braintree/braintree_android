package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.BraintreeApi;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class AndroidPayCard extends PaymentMethod implements Parcelable, Serializable {

    protected static final String PAYMENT_METHOD_TYPE = "AndroidPayCard";

    @SerializedName("details")
    private AndroidPayCardDetails mDetails;

    @Override
    public String getTypeLabel() {
        return "Android Pay";
    }

    public String getLastTwo() {
        return mDetails.getLastTwo();
    }

    /**
     * Required for and handled by {@link com.braintreepayments.api.Braintree}. Not intended for general consumption.
     * @param androidPayCard Raw JSON representation of a {@link com.braintreepayments.api.models.Card}.
     * @return {@link AndroidPayCard} for use in payment method selection UIs.
     */
    public static AndroidPayCard fromJson(String androidPayCard) {
        return new Gson().fromJson(androidPayCard, AndroidPayCard.class);
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

    public AndroidPayCard() {}

    private AndroidPayCard(Parcel in) {
        mDetails = in.readParcelable(AndroidPayCardDetails.class.getClassLoader());
        mNonce = in.readString();
        mDescription = in.readString();
        mPaymentMethodOptions= (PaymentMethodOptions) in.readSerializable();
        mSource = in.readString();
    }

    public static final Creator<AndroidPayCard> CREATOR = new Creator<AndroidPayCard>() {
        public AndroidPayCard createFromParcel(Parcel source) {
            return new AndroidPayCard(source);
        }

        public AndroidPayCard[] newArray(int size) {return new AndroidPayCard[size];}
    };

    private static class AndroidPayCardDetails implements Parcelable, Serializable {

        @SerializedName("cardType") private String mCardType;
        @SerializedName("lastTwo") private String mLastTwo;

        public AndroidPayCardDetails() {}

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

        private AndroidPayCardDetails(Parcel in) {
            mCardType = in.readString();
            mLastTwo = in.readString();
        }

        public static final Creator<AndroidPayCardDetails> CREATOR = new Creator<AndroidPayCardDetails>() {
            public AndroidPayCardDetails createFromParcel(Parcel source) {return new AndroidPayCardDetails(source);}

            public AndroidPayCardDetails[] newArray(int size) {return new AndroidPayCardDetails[size];}
        };
    }
}
