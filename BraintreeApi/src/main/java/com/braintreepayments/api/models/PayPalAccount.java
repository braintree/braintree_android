package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link com.braintreepayments.api.models.PaymentMethod} representing a PayPal account.
 *
 * @see com.braintreepayments.api.models.Card
 * @see PaymentMethod
 */
public class PayPalAccount extends PaymentMethod implements Parcelable {

    protected static final String PAYMENT_METHOD_TYPE = "PayPalAccount";
    protected static final String API_RESOURCE_KEY = "paypalAccounts";

    private static final String DETAILS_KEY = "details";
    private static final String EMAIL_KEY = "email";
    private static final String PAYER_INFO_KEY = "payerInfo";
    private static final String ACCOUNT_ADDRESS_KEY = "accountAddress";
    private static final String SHIPPING_ADDRESS_KEY = "shippingAddress";
    private static final String BILLING_ADDRESS_KEY = "billingAddress";
    private static final String FIRST_NAME_KEY = "firstName";
    private static final String LAST_NAME_KEY = "lastName";
    private static final String PHONE_KEY = "phone";
    private static final String PAYER_ID_KEY = "payer_id";

    private String mClientMetadataId;
    private PostalAddress mBillingAddress;
    private PostalAddress mShippingAddress;
    private String mFirstName;
    private String mLastName;
    private String mPhone;
    private String mEmail;
    private String mPayerId;

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

    /**
     * Generates a {@link PayPalAccount} from the {@link JSONObject}.
     *
     * @param json {@link JSONObject} that holds properties for {@link PayPalAccount}.
     * @throws JSONException
     */
    protected void fromJson(JSONObject json) throws JSONException {
        super.fromJson(json);

        JSONObject details = json.getJSONObject(DETAILS_KEY);
        mEmail = details.optString(EMAIL_KEY, null);

        try {
            JSONObject payerInfo = details.getJSONObject(PAYER_INFO_KEY);
            JSONObject billingAddress = payerInfo.has(ACCOUNT_ADDRESS_KEY) ? payerInfo.optJSONObject(ACCOUNT_ADDRESS_KEY) : payerInfo.optJSONObject(BILLING_ADDRESS_KEY);
            JSONObject shippingAddress = payerInfo.optJSONObject(SHIPPING_ADDRESS_KEY);

            mBillingAddress = PostalAddress.fromJson(billingAddress);
            mShippingAddress = PostalAddress.fromJson(shippingAddress);

            mFirstName = payerInfo.optString(FIRST_NAME_KEY);
            mLastName = payerInfo.optString(LAST_NAME_KEY);
            mPhone = payerInfo.optString(PHONE_KEY);
            mPayerId = payerInfo.optString(PAYER_ID_KEY);

            if(mEmail == null) {
                mEmail = payerInfo.optString(EMAIL_KEY, null);
            }
        } catch (JSONException ignored) {}
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

    /**
     * @return The shipping address of the user provided by checkout flows.
     */
    public PostalAddress getShippingAddress() {
        return mShippingAddress;
    }

    /**
     * @return The first name associated with the PayPal account.
     */
    public String getFirstName() {
        return mFirstName;
    }

    /**
     * @return The last name associated with the PayPal account.
     */
    public String getLastName() {
        return mLastName;
    }

    /**
     * @return The phone number associated with the PayPal account.
     */
    public String getPhone() {
        return mPhone;
    }

    /**
     * @return The ClientMetadataId associated with this transaction.
     */
    public String getClientMetadataId(){
        return mClientMetadataId;
    }

    /**
     * @return The Payer ID provided in checkout flows.
     */
    public String getPayerId(){
        return mPayerId;
    }

    /**
     * Sets the ClientMetadataId after the app switch
     * @param clientMetadataId - Client Metadata Id
     */
    protected void setClientMetadataId(String clientMetadataId) {
        mClientMetadataId = clientMetadataId;
    }

    public PayPalAccount() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mClientMetadataId);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeParcelable(mBillingAddress, flags);
        dest.writeParcelable(mShippingAddress, flags);
        dest.writeString(mFirstName);
        dest.writeString(mLastName);
        dest.writeString(mEmail);
        dest.writeString(mPhone);
        dest.writeString(mPayerId);
    }

    private PayPalAccount(Parcel in) {
        mClientMetadataId = in.readString();
        mNonce = in.readString();
        mDescription = in.readString();
        mBillingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        mShippingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        mFirstName = in.readString();
        mLastName = in.readString();
        mEmail = in.readString();
        mPhone = in.readString();
        mPayerId = in.readString();
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
