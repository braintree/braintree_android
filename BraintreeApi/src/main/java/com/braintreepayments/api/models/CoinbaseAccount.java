package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.annotations.Beta;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * {@link com.braintreepayments.api.models.PaymentMethod} representing a Coinbase account.
 *
 * @see {@link com.braintreepayments.api.models.PayPalAccount}
 * @see {@link com.braintreepayments.api.models.PaymentMethod}
 */
@Beta
public class CoinbaseAccount extends PaymentMethod implements Parcelable, Serializable {

    protected static final String PAYMENT_METHOD_TYPE = "CoinbaseAccount";

    @SerializedName("code")
    private String mAccessCode;

    @SerializedName("details")
    private CoinbaseAccountDetails mDetails;

    @SerializedName("redirect_uri")
    private String mRedirectUri;

    protected void setAccessCode(String accessCode) {
        mAccessCode = accessCode;
    }

    protected void setRedirectUri(String redirectUri) {
        mRedirectUri = redirectUri;
    }

    /**
     * @return The email address associated with this Coinbase account.
     */
    public String getEmail() {
        if (mDetails != null) {
            return mDetails.getEmail();
        } else {
            return "";
        }
    }

    /**
     * @return The type of this {@link com.braintreepayments.api.models.PaymentMethod} (always "Coinbase").
     */
    @Override
    public String getTypeLabel() {
        return "Coinbase";
    }

    /**
     * Required for and handled by {@link com.braintreepayments.api.Braintree}. Not intended for general consumption.
     *
     * @param json Raw JSON representation of a {@link com.braintreepayments.api.models.CoinbaseAccount}.
     * @return {@link com.braintreepayments.api.models.CoinbaseAccount} for use in payment method selection UIs.
     */
    public static CoinbaseAccount fromJson(String json) {
        return new Gson().fromJson(json, CoinbaseAccount.class);
    }

    public CoinbaseAccount() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mAccessCode);
        dest.writeString(mRedirectUri);
        dest.writeParcelable(mDetails, 0);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeSerializable(mPaymentMethodOptions);
        dest.writeString(mSource);
    }

    private CoinbaseAccount(Parcel in) {
        mAccessCode = in.readString();
        mRedirectUri = in.readString();
        mDetails = in.readParcelable(CoinbaseAccountDetails.class.getClassLoader());
        mNonce = in.readString();
        mDescription = in.readString();
        mPaymentMethodOptions = (PaymentMethodOptions) in.readSerializable();
        mSource = in.readString();
    }

    public static final Creator<CoinbaseAccount> CREATOR = new Creator<CoinbaseAccount>() {
        public CoinbaseAccount createFromParcel(Parcel source) {return new CoinbaseAccount(source);}

        public CoinbaseAccount[] newArray(int size) {return new CoinbaseAccount[size];}
    };

    private static class CoinbaseAccountDetails implements Parcelable, Serializable {

        @SerializedName("email")
        private String mEmail;

        public String getEmail() {
            return mEmail;
        }

        public CoinbaseAccountDetails() {}

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.mEmail);
        }

        private CoinbaseAccountDetails(Parcel in) {
            this.mEmail = in.readString();
        }

        public static final Creator<CoinbaseAccountDetails> CREATOR =
                new Creator<CoinbaseAccountDetails>() {
                    public CoinbaseAccountDetails createFromParcel(Parcel source) {
                        return new CoinbaseAccountDetails(source);
                    }

                    public CoinbaseAccountDetails[] newArray(int size) {
                        return new CoinbaseAccountDetails[size];
                    }
                };
    }
}
