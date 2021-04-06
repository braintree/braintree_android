package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link UntypedPaymentMethodNonce} representing a local payment.
 *
 * @see UntypedPaymentMethodNonce
 */
public class LocalPaymentNonce implements PaymentMethodNonce {

    static final String API_RESOURCE_KEY = "paypalAccounts";

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";
    private static final String DESCRIPTION_KEY = "description";

    private static final String DETAILS_KEY = "details";
    private static final String EMAIL_KEY = "email";
    private static final String PAYER_INFO_KEY = "payerInfo";
    private static final String ACCOUNT_ADDRESS_KEY = "accountAddress";
    private static final String SHIPPING_ADDRESS_KEY = "shippingAddress";
    private static final String BILLING_ADDRESS_KEY = "billingAddress";
    private static final String FIRST_NAME_KEY = "firstName";
    private static final String LAST_NAME_KEY = "lastName";
    private static final String PHONE_KEY = "phone";
    private static final String PAYER_ID_KEY = "payerId";
    private static final String CLIENT_METADATA_ID_KEY = "correlationId";
    private static final String TYPE_KEY = "type";

    private String mClientMetadataId;
    private PostalAddress mBillingAddress;
    private PostalAddress mShippingAddress;
    private String mGivenName;
    private String mSurname;
    private String mPhone;
    private String mEmail;
    private String mPayerId;
    private String mType;

    protected String mNonce;
    protected String mDescription;
    protected boolean mDefault;

    LocalPaymentNonce(String jsonString) throws JSONException {
        this(new JSONObject(jsonString));
    }

    LocalPaymentNonce(JSONObject inputJson) throws JSONException {
        JSONObject json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        JSONObject details = json.getJSONObject(DETAILS_KEY);
        mNonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        mDescription = json.getString(DESCRIPTION_KEY);
        mDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
        mEmail = Json.optString(details, EMAIL_KEY, null);
        mClientMetadataId = Json.optString(details, CLIENT_METADATA_ID_KEY, null);
        mType = Json.optString(json, TYPE_KEY, "PayPalAccount");

        try {
            JSONObject payerInfo = details.getJSONObject(PAYER_INFO_KEY);

            JSONObject billingAddress;
            if (payerInfo.has(ACCOUNT_ADDRESS_KEY)) {
                billingAddress = payerInfo.optJSONObject(ACCOUNT_ADDRESS_KEY);
            } else {
                billingAddress = payerInfo.optJSONObject(BILLING_ADDRESS_KEY);
            }

            JSONObject shippingAddress = payerInfo.optJSONObject(SHIPPING_ADDRESS_KEY);

            mBillingAddress = PostalAddressParser.fromJson(billingAddress);
            mShippingAddress = PostalAddressParser.fromJson(shippingAddress);

            mGivenName = Json.optString(payerInfo, FIRST_NAME_KEY, "");
            mSurname = Json.optString(payerInfo, LAST_NAME_KEY, "");
            mPhone = Json.optString(payerInfo, PHONE_KEY, "");
            mPayerId = Json.optString(payerInfo, PAYER_ID_KEY, "");

            if(mEmail == null) {
                mEmail = Json.optString(payerInfo, EMAIL_KEY, null);
            }
        } catch (JSONException e) {
            mBillingAddress = new PostalAddress();
            mShippingAddress = new PostalAddress();
        }
    }

    /**
     * @return The email address associated with this local payment
     */
    public String getEmail() {
        return mEmail;
    }

    @Override
    public String getNonce() {
        return mNonce;
    }

    /**
     * @return The description of this local payment for displaying to a customer
     */
    @Override
    public String getDescription() {
        return mDescription;
    }

    @Override
    public boolean isDefault() {
        return mDefault;
    }

    /**
     * @return The type of this {@link UntypedPaymentMethodNonce}
     */
    @Override
    public String getTypeLabel() {
        return mType;
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
     * @return The first name associated with the local payment.
     */
    public String getGivenName() {
        return mGivenName;
    }

    /**
     * @return The last name associated with the local payment.
     */
    public String getSurname() {
        return mSurname;
    }

    /**
     * @return The phone number associated with the local payment.
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
     * @return The Payer ID provided in local payment flows.
     */
    public String getPayerId(){
        return mPayerId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mClientMetadataId);
        dest.writeParcelable(mBillingAddress, flags);
        dest.writeParcelable(mShippingAddress, flags);
        dest.writeString(mGivenName);
        dest.writeString(mSurname);
        dest.writeString(mEmail);
        dest.writeString(mPhone);
        dest.writeString(mPayerId);
        dest.writeString(mType);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeByte(mDefault ? (byte) 1 : (byte) 0);
    }

    private LocalPaymentNonce(Parcel in) {
        mClientMetadataId = in.readString();
        mBillingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        mShippingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        mGivenName = in.readString();
        mSurname = in.readString();
        mEmail = in.readString();
        mPhone = in.readString();
        mPayerId = in.readString();
        mType = in.readString();
        mNonce = in.readString();
        mDescription = in.readString();
        mDefault = in.readByte() > 0;
    }

    public static final Creator<LocalPaymentNonce> CREATOR = new Creator<LocalPaymentNonce>() {
        public LocalPaymentNonce createFromParcel(Parcel source) {
            return new LocalPaymentNonce(source);
        }

        public LocalPaymentNonce[] newArray(int size) {
            return new LocalPaymentNonce[size];
        }
    };
}
