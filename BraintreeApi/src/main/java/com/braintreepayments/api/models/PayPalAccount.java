package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.braintreepayments.api.Utils;

import java.io.Serializable;

/**
 * {@link com.braintreepayments.api.models.PaymentMethod} representing a PayPal account.
 * @see {@link com.braintreepayments.api.models.Card}
 * @see {@link com.braintreepayments.api.models.PaymentMethod}
 */
public class PayPalAccount extends PaymentMethod implements Parcelable, Serializable {

    protected static final String PAYMENT_METHOD_TYPE = "PayPalAccount";

    private String consentCode;
    private String correlationId;
    private PayPalDetails details;

    public PayPalAccount() {}

    protected void setEmail(String email) {
        details = new PayPalDetails();
        details.setEmail(email);
    }

    protected void setConsentCode(String consentCode) {
        this.consentCode = consentCode;
    }

    protected void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * @return The email address associated with this PayPal account
     */
    public String getEmail() {
        if (details != null) {
            return details.getEmail();
        } else {
            return "";
        }
    }

    /**
     * @return The description of this PayPal account for displaying to a customer, either email or
     * 'PayPal'
     */
    @Override
    public String getDescription() {
        if (TextUtils.equals(super.getDescription(), "PayPal") && !TextUtils.isEmpty(getEmail())) {
            return getEmail();
        } else {
            return super.getDescription();
        }
    }

    /**
     * @return The type of this {@link com.braintreepayments.api.models.PaymentMethod} (always "PayPal")
     */
    @Override
    public String getTypeLabel() {
        return "PayPal";
    }

    /**
     * Required for and handled by {@link com.braintreepayments.api.Braintree}. Not intended for general consumption.
     * @param json Raw JSON representation of a {@link com.braintreepayments.api.models.PayPalAccount}.
     * @return {@link com.braintreepayments.api.models.PayPalAccount} for use in payment method selection UIs.
     */
    public static PayPalAccount fromJson(String json) {
        return Utils.getGson().fromJson(json, PayPalAccount.class);
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.consentCode);
        dest.writeString(this.correlationId);
        dest.writeParcelable(this.details, 0);
        dest.writeString(this.nonce);
        dest.writeString(this.description);
        dest.writeSerializable(this.options);
        dest.writeString(this.mSource);
    }

    private PayPalAccount(Parcel in) {
        this.consentCode = in.readString();
        this.correlationId = in.readString();
        this.details = in.readParcelable(PayPalDetails.class.getClassLoader());
        this.nonce = in.readString();
        this.description = in.readString();
        this.options = (PaymentMethodOptions) in.readSerializable();
        this.mSource = in.readString();
    }

    public static final Creator<PayPalAccount> CREATOR = new Creator<PayPalAccount>() {
        public PayPalAccount createFromParcel(Parcel source) {return new PayPalAccount(source);}

        public PayPalAccount[] newArray(int size) {return new PayPalAccount[size];}
    };

    private static class PayPalDetails implements Parcelable, Serializable {
        private String email;

        public PayPalDetails() {}

        private String getEmail() {
            return email;
        }

        private void setEmail(String email) {
            this.email = email;
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {dest.writeString(this.email);}

        private PayPalDetails(Parcel in) {this.email = in.readString();}

        public static final Creator<PayPalDetails> CREATOR = new Creator<PayPalDetails>() {
            public PayPalDetails createFromParcel(Parcel source) {return new PayPalDetails(source);}

            public PayPalDetails[] newArray(int size) {return new PayPalDetails[size];}
        };
    }

}
