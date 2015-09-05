package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * {@link com.braintreepayments.api.models.PaymentMethod} representing a PayPal account.
 * @see {@link com.braintreepayments.api.models.Card}
 * @see {@link com.braintreepayments.api.models.PaymentMethod}
 */
public class PayPalAccount extends PaymentMethod implements Parcelable, Serializable {

    protected static final String PAYMENT_METHOD_TYPE = "PayPalAccount";

    @SerializedName("consentCode") private String mConsentCode;
    @SerializedName("correlationId") private String mCorrelationId;
    @SerializedName("details") private PayPalDetails mDetails;

    // This is intentionally not serialized because it's not used by the Gateway.
    private PostalAddress mBillingAddress;

    public PayPalAccount() {}

    protected void setEmail(String email) {
        mDetails = new PayPalDetails();
        mDetails.setEmail(email);
    }

    public PostalAddress getBillingAddress() {
        return mBillingAddress;
    }

    protected void setConsentCode(String consentCode) {
        mConsentCode = consentCode;
    }

    protected void setCorrelationId(String correlationId) {
        mCorrelationId = correlationId;
    }

    /**
     * @return The email address associated with this PayPal account
     */
    public String getEmail() {
        if (mDetails != null) {
            return mDetails.getEmail();
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
        PayPalAccount payPalAccount = new Gson().fromJson(json, PayPalAccount.class);
        try {
            String accountAddressJson = new JSONObject(json).getJSONObject("details")
                    .getJSONObject("payerInfo").getJSONObject("accountAddress").toString();
            payPalAccount.mBillingAddress = new Gson().fromJson(accountAddressJson, PostalAddress.class);
        } catch (JSONException ignored) {
            // Absence of address info shouldn't block a payment method from being created
        }

        return payPalAccount;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mConsentCode);
        dest.writeString(mCorrelationId);
        dest.writeParcelable(mDetails, 0);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeSerializable(mPaymentMethodOptions);
        dest.writeString(mSource);
        dest.writeParcelable(mBillingAddress, 0);
    }

    private PayPalAccount(Parcel in) {
        mConsentCode = in.readString();
        mCorrelationId = in.readString();
        mDetails = in.readParcelable(PayPalDetails.class.getClassLoader());
        mNonce = in.readString();
        mDescription = in.readString();
        mPaymentMethodOptions = (PaymentMethodOptions) in.readSerializable();
        mSource = in.readString();
        mBillingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
    }

    public static final Creator<PayPalAccount> CREATOR = new Creator<PayPalAccount>() {
        public PayPalAccount createFromParcel(Parcel source) {return new PayPalAccount(source);}

        public PayPalAccount[] newArray(int size) {return new PayPalAccount[size];}
    };

    private static class PayPalDetails implements Parcelable, Serializable {

        @SerializedName("email") private String mEmail;

        public PayPalDetails() {}

        private String getEmail() {
            return mEmail;
        }

        private void setEmail(String email) {
            mEmail = email;
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {dest.writeString(mEmail);}

        private PayPalDetails(Parcel in) {mEmail = in.readString();}

        public static final Creator<PayPalDetails> CREATOR = new Creator<PayPalDetails>() {
            public PayPalDetails createFromParcel(Parcel source) {return new PayPalDetails(source);}

            public PayPalDetails[] newArray(int size) {return new PayPalDetails[size];}
        };
    }
}
