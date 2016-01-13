package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.annotations.Beta;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * {@link com.braintreepayments.api.models.PaymentMethod} representing a credit or debit card.
 * @see com.braintreepayments.api.models.PaymentMethod
 * @see com.braintreepayments.api.models.PayPalAccount
 */
public class Card extends PaymentMethod implements Parcelable, Serializable {

    protected static final String PAYMENT_METHOD_TYPE = "CreditCard";

    @SerializedName("billingAddress") private BillingAddress mBillingAddress;
    @SerializedName("threeDSecureInfo") private ThreeDSecureInfo mThreeDSecureInfo;
    @SerializedName("details") private CardDetails mDetails;
    @SerializedName("expirationMonth") private String mExpirationMonth;
    @SerializedName("expirationYear") private String mExpirationYear;
    @SerializedName("expirationDate") private String mExpirationDate;
    @SerializedName("number") private String mNumber;
    @SerializedName("cvv") private String mCvv;
    @SerializedName("cardholderName") private String mCardholderName;

    public Card() {}

    /**
     * @return Type of this card (e.g. MasterCard, American Express)
     */
    @Override
    public String getTypeLabel() {
        return mDetails.getCardType();
    }

    /**
     * @return Last two digits of the card, intended for display purposes.
     */
    public String getLastTwo() {
        return mDetails.getLastTwo();
    }

    /**
     * @return The 3D Secure info for the current {@link com.braintreepayments.api.models.Card} or
     * {@code null}
     */
    @Beta
    public ThreeDSecureInfo getThreeDSecureInfo() {
        return mThreeDSecureInfo;
    }

    protected void setBillingAddress(BillingAddress billingAddress) {
        mBillingAddress = billingAddress;
    }

    @Beta
    protected void setThreeDSecureInfo(ThreeDSecureInfo threeDSecureInfo) {
        mThreeDSecureInfo = threeDSecureInfo;
    }

    protected void setExpirationMonth(String expirationMonth) {
        mExpirationMonth = expirationMonth;
    }

    protected void setExpirationYear(String expirationYear) {
        mExpirationYear = expirationYear;
    }

    protected void setExpirationDate(String expirationDate) {
        mExpirationDate = expirationDate;
    }

    protected void setCardNumber(String number) {
        mNumber = number;
    }

    protected void setCvv(String cvv) {
        mCvv = cvv;
    }

    protected void setCardholderName(String cardholderName) {
        mCardholderName = cardholderName;
    }

    /**
     * Required for and handled by {@link com.braintreepayments.api.Braintree}. Not intended for general consumption.
     * @param creditCard Raw JSON representation of a {@link com.braintreepayments.api.models.Card}.
     * @return {@link com.braintreepayments.api.models.Card} for use in payment method selection UIs.
     */
    public static Card fromJson(String creditCard) {
        return new Gson().fromJson(creditCard, Card.class);
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mBillingAddress, 0);
        dest.writeParcelable(mThreeDSecureInfo, 0);
        dest.writeParcelable(mDetails, 0);
        dest.writeString(mExpirationMonth);
        dest.writeString(mExpirationYear);
        dest.writeString(mExpirationDate);
        dest.writeString(mNumber);
        dest.writeString(mCvv);
        dest.writeString(mCardholderName);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeSerializable(mPaymentMethodOptions);
        dest.writeString(mSource);
    }

    private Card(Parcel in) {
        mBillingAddress = in.readParcelable(BillingAddress.class.getClassLoader());
        mThreeDSecureInfo = in.readParcelable(ThreeDSecureInfo.class.getClassLoader());
        mDetails = in.readParcelable(CardDetails.class.getClassLoader());
        mExpirationMonth = in.readString();
        mExpirationYear = in.readString();
        mExpirationDate = in.readString();
        mNumber = in.readString();
        mCvv = in.readString();
        mCardholderName = in.readString();
        mNonce = in.readString();
        mDescription = in.readString();
        mPaymentMethodOptions = (PaymentMethodOptions) in.readSerializable();
        mSource = in.readString();
    }

    public static final Creator<Card> CREATOR = new Creator<Card>() {
        public Card createFromParcel(Parcel source) {return new Card(source);}

        public Card[] newArray(int size) {return new Card[size];}
    };

    private static class CardDetails implements Parcelable, Serializable {

        @SerializedName("cardType") private String mCardType;
        @SerializedName("lastTwo") private String mLastTwo;

        public CardDetails() {}

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

        private CardDetails(Parcel in) {
            mCardType = in.readString();
            mLastTwo = in.readString();
        }

        public static final Creator<CardDetails> CREATOR = new Creator<CardDetails>() {
            public CardDetails createFromParcel(Parcel source) {return new CardDetails(source);}

            public CardDetails[] newArray(int size) {return new CardDetails[size];}
        };
    }

    protected static class BillingAddress implements Parcelable, Serializable {

        @SerializedName("firstName") private String mFirstName;
        @SerializedName("lastName") private String mLastName;
        @SerializedName("countryName") private String mCountryName;
        @SerializedName("locality") private String mLocality;
        @SerializedName("postalCode") private String mPostalCode;
        @SerializedName("region") private String mRegion;
        @SerializedName("streetAddress") private String mStreetAddress;

        public BillingAddress() {}

        protected void setFirstName(String firstName) {
            mFirstName = firstName;
        }

        protected void setLastName(String lastName) {
            mLastName = lastName;
        }

        protected void setCountryName(String countryName) {
            mCountryName = countryName;
        }

        protected void setLocality(String locality) {
            mLocality = locality;
        }

        protected void setPostalCode(String postalCode) {
            mPostalCode = postalCode;
        }

        protected void setRegion(String region) {
            mRegion = region;
        }

        protected void setStreetAddress(String streetAddress) {
            mStreetAddress = streetAddress;
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mFirstName);
            dest.writeString(mLastName);
            dest.writeString(mCountryName);
            dest.writeString(mLocality);
            dest.writeString(mPostalCode);
            dest.writeString(mRegion);
            dest.writeString(mStreetAddress);
        }

        private BillingAddress(Parcel in) {
            mFirstName = in.readString();
            mLastName = in.readString();
            mCountryName = in.readString();
            mLocality = in.readString();
            mPostalCode = in.readString();
            mRegion = in.readString();
            mStreetAddress = in.readString();
        }

        public static final Creator<BillingAddress> CREATOR = new Creator<BillingAddress>() {
            public BillingAddress createFromParcel(Parcel source) {
                return new BillingAddress(source);
            }

            public BillingAddress[] newArray(int size) {return new BillingAddress[size];}
        };
    }
}