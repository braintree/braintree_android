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

    // These are intentionally not serialized because they're not used by the Gateway.
    private PostalAddress mBillingAddress;
    private PostalAddress mShippingAddress;
    private String mFirstName;
    private String mLastName;
    private String mPhone;
    private String mEmail;


    public PayPalAccount() {}

    protected void setEmail(String email) {
        mEmail = email;
    }

    public PostalAddress getBillingAddress() {
        return mBillingAddress;
    }

    public PostalAddress getShippingAddress() {
        return mShippingAddress;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public String getPhone() {
        return mPhone;
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
        if (mEmail == null) { return ""; }
        return mEmail;
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
            payPalAccount.mEmail = new JSONObject(json).optJSONObject("details")
                    .optString("email", null); // Allow for email outside payerInfo
            JSONObject payerInfo = new JSONObject(json).optJSONObject("details")
                    .optJSONObject("payerInfo");
            if (payerInfo != null) {
                JSONObject addressJson = payerInfo.optJSONObject("accountAddress");
                payPalAccount.mBillingAddress = PostalAddress.addressFromJson(addressJson);
                if (payPalAccount.mBillingAddress == null) {
                    addressJson = payerInfo.optJSONObject("billingAddress");
                    payPalAccount.mBillingAddress = PostalAddress.addressFromJson(addressJson);
                }
                addressJson = payerInfo.optJSONObject("shippingAddress");
                payPalAccount.mShippingAddress = PostalAddress.addressFromJson(addressJson);
                payPalAccount.mFirstName = payerInfo.optString("firstName");
                payPalAccount.mLastName = payerInfo.optString("lastName");
                payPalAccount.mPhone = payerInfo.optString("phone");
                if (payPalAccount.mEmail == null) {
                    payPalAccount.mEmail = payerInfo.optString("email", null);
                }
            }
        } catch (JSONException ignored) { }

        return payPalAccount;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mConsentCode);
        dest.writeString(mCorrelationId);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeSerializable(mPaymentMethodOptions);
        dest.writeString(mSource);
        dest.writeParcelable(mBillingAddress, 0);
        dest.writeParcelable(mShippingAddress, 0);
        dest.writeString(mFirstName);
        dest.writeString(mLastName);
        dest.writeString(mEmail);
        dest.writeString(mPhone);
    }

    private PayPalAccount(Parcel in) {
        mConsentCode = in.readString();
        mCorrelationId = in.readString();
        mNonce = in.readString();
        mDescription = in.readString();
        mPaymentMethodOptions = (PaymentMethodOptions) in.readSerializable();
        mSource = in.readString();
        mBillingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        mShippingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        mFirstName = in.readString();
        mLastName = in.readString();
        mEmail = in.readString();
        mPhone = in.readString();
    }

    public static final Creator<PayPalAccount> CREATOR = new Creator<PayPalAccount>() {
        public PayPalAccount createFromParcel(Parcel source) {return new PayPalAccount(source);}

        public PayPalAccount[] newArray(int size) {return new PayPalAccount[size];}
    };

}
