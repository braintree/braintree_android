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

    private BillingAddress billingAddress;
    private ThreeDSecureInfo threeDSecureInfo;
    private CardDetails details;
    private String expirationMonth;
    private String expirationYear;
    private String expirationDate;
    @SerializedName("number") private String cardNumber;
    private String cvv;

    public Card() {}

    /**
     * @return Type of this card (e.g. MasterCard, American Express)
     */
    @Override
    public String getTypeLabel() {
        return details.getCardType();
    }

    /**
     * @return Last two digits of the card, intended for display purposes.
     */
    public String getLastTwo() {
        return details.getLastTwo();
    }

    /**
     * @return The 3D Secure info for the current {@link com.braintreepayments.api.models.Card} or
     * {@code null}
     */
    @Beta
    public ThreeDSecureInfo getThreeDSecureInfo() {
        return threeDSecureInfo;
    }

    protected void setBillingAddress(BillingAddress billingAddress) {
        this.billingAddress = billingAddress;
    }

    @Beta
    protected void setThreeDSecureInfo(ThreeDSecureInfo threeDSecureInfo) {
        this.threeDSecureInfo = threeDSecureInfo;
    }

    protected void setExpirationMonth(String expirationMonth) {
        this.expirationMonth = expirationMonth;
    }

    protected void setExpirationYear(String expirationYear) {
        this.expirationYear = expirationYear;
    }

    protected void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    protected void setCardNumber(String number) {
        this.cardNumber = number;
    }

    protected void setCvv(String cvv) {
        this.cvv = cvv;
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
        dest.writeParcelable(this.billingAddress, 0);
        dest.writeParcelable(this.details, 0);
        dest.writeString(this.expirationMonth);
        dest.writeString(this.expirationYear);
        dest.writeString(this.expirationDate);
        dest.writeString(this.cardNumber);
        dest.writeString(this.cvv);
        dest.writeString(this.nonce);
        dest.writeString(this.description);
        dest.writeSerializable(this.options);
        dest.writeString(this.mSource);
    }

    private Card(Parcel in) {
        this.billingAddress = in.readParcelable(BillingAddress.class.getClassLoader());
        this.details = in.readParcelable(CardDetails.class.getClassLoader());
        this.expirationMonth = in.readString();
        this.expirationYear = in.readString();
        this.expirationDate = in.readString();
        this.cardNumber = in.readString();
        this.cvv = in.readString();
        this.nonce = in.readString();
        this.description = in.readString();
        this.options = (PaymentMethodOptions) in.readSerializable();
        this.mSource = in.readString();
    }

    public static final Creator<Card> CREATOR = new Creator<Card>() {
        public Card createFromParcel(Parcel source) {return new Card(source);}

        public Card[] newArray(int size) {return new Card[size];}
    };

    private static class CardDetails implements Parcelable, Serializable {
        private String cardType;
        private String lastTwo;

        public CardDetails() {}

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

        private CardDetails(Parcel in) {
            this.cardType = in.readString();
            this.lastTwo = in.readString();
        }

        public static final Creator<CardDetails> CREATOR = new Creator<CardDetails>() {
            public CardDetails createFromParcel(Parcel source) {return new CardDetails(source);}

            public CardDetails[] newArray(int size) {return new CardDetails[size];}
        };
    }

    protected static class BillingAddress implements Parcelable, Serializable {
        private String firstName;
        private String lastName;
        private String countryName;
        private String locality;
        private String postalCode;
        private String region;
        private String streetAddress;

        public BillingAddress() {}

        protected void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        protected void setLastName(String lastName) {
            this.lastName = lastName;
        }

        protected void setCountryName(String countryName) {
            this.countryName = countryName;
        }

        protected void setLocality(String locality) {
            this.locality = locality;
        }

        protected void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }

        protected void setRegion(String region) {
            this.region = region;
        }

        protected void setStreetAddress(String streetAddress) {
            this.streetAddress = streetAddress;
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.firstName);
            dest.writeString(this.lastName);
            dest.writeString(this.countryName);
            dest.writeString(this.locality);
            dest.writeString(this.postalCode);
            dest.writeString(this.region);
            dest.writeString(this.streetAddress);
        }

        private BillingAddress(Parcel in) {
            this.firstName = in.readString();
            this.lastName = in.readString();
            this.countryName = in.readString();
            this.locality = in.readString();
            this.postalCode = in.readString();
            this.region = in.readString();
            this.streetAddress = in.readString();
        }

        public static final Creator<BillingAddress> CREATOR = new Creator<BillingAddress>() {
            public BillingAddress createFromParcel(Parcel source) {
                return new BillingAddress(source);
            }

            public BillingAddress[] newArray(int size) {return new BillingAddress[size];}
        };
    }
}