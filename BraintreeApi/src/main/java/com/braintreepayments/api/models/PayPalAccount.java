package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link com.braintreepayments.api.models.PaymentMethod} representing a PayPal account.
 * @see {@link com.braintreepayments.api.models.Card}
 * @see {@link com.braintreepayments.api.models.PaymentMethod}
 */
public class PayPalAccount extends PaymentMethod implements Parcelable {

    protected static final String PAYMENT_METHOD_TYPE = "PayPalAccount";

    private static final String API_RESOURCE_KEY = "paypalAccounts";
    private static final String DETAILS_KEY = "details";
    private static final String EMAIL_KEY = "email";
    private static final String PAYER_INFO_KEY = "payerInfo";
    private static final String ACCOUNT_ADDRESS_KEY = "accountAddress";
    private static final String STREET_ADDRESS_KEY = "street1";
    private static final String EXTENDED_ADDRESS_KEY = "street2";
    private static final String LOCALITY_KEY = "city";
    private static final String COUNTRY_CODE_ALPHA_2_KEY = "country";
    private static final String POSTAL_CODE_KEY = "postalCode";
    private static final String REGION_KEY = "state";

    private String mEmail;
    private PostalAddress mBillingAddress;

    /**
     * Convert an API response to a {@link PayPalAccount}.
     *
     * @param json Raw JSON representation of a {@link PayPalAccount}.
     * @return {@link PayPalAccount} for use in payment method selection UIs.
     */
    public static PayPalAccount fromJson(String json) throws JSONException {
        PayPalAccount payPalAccount = new PayPalAccount();
        payPalAccount.fromJson(PayPalAccount.getJsonObjectForType(API_RESOURCE_KEY, json));
        return payPalAccount;
    }

    protected void fromJson(JSONObject json) throws JSONException {
        super.fromJson(json);

        JSONObject details = json.getJSONObject(DETAILS_KEY);
        mEmail = details.getString(EMAIL_KEY);

        try {
            JSONObject accountAddress = details
                    .getJSONObject(PAYER_INFO_KEY)
                    .getJSONObject(ACCOUNT_ADDRESS_KEY);
            String streetAddress = accountAddress.optString(STREET_ADDRESS_KEY, null);
            String extendedAddress = accountAddress.optString(EXTENDED_ADDRESS_KEY, null);
            String locality = accountAddress.optString(LOCALITY_KEY, null);
            String region = accountAddress.optString(REGION_KEY, null);
            String postalCode = accountAddress.optString(POSTAL_CODE_KEY, null);
            String countryCodeAlpha2 = accountAddress.optString(COUNTRY_CODE_ALPHA_2_KEY, null);
            mBillingAddress = new PostalAddress(streetAddress, extendedAddress,
                    locality, region, postalCode, countryCodeAlpha2);
        } catch (JSONException e) {
            mBillingAddress = new PostalAddress();
        }
    }

    /**
     * @return The email address associated with this PayPal account
     */
    public String getEmail() {
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
     * @return The type of this {@link PaymentMethod} (always "PayPal")
     */
    @Override
    public String getTypeLabel() {
        return "PayPal";
    }

    /**
     * @return The billing address of the user if requested with additional scopes.
     */
    public PostalAddress getBillingAddress() {
        return mBillingAddress;
    }

    public PayPalAccount() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeString(mEmail);
        dest.writeParcelable(mBillingAddress, flags);
    }

    private PayPalAccount(Parcel in) {
        mNonce = in.readString();
        mDescription = in.readString();
        mEmail = in.readString();
        mBillingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
    }

    public static final Creator<PayPalAccount> CREATOR = new Creator<PayPalAccount>() {
        public PayPalAccount createFromParcel(Parcel source) {
            return new PayPalAccount(source);
        }

        public PayPalAccount[] newArray(int size) {
            return new PayPalAccount[size];
        }
    };
}
