package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link BraintreeNonce} representing a local payment.
 *
 * @see BraintreeNonce
 */
public class LocalPaymentNonce extends BraintreeNonce {

    private static final String API_RESOURCE_KEY = "paypalAccounts";

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

    private final String mClientMetadataId;
    private PostalAddress mBillingAddress;
    private PostalAddress mShippingAddress;
    private String mGivenName;
    private String mSurname;
    private String mPhone;
    private String mEmail;
    private String mPayerId;
    private final String mType;

    private final String mNonce;
    private final String mDescription;
    private final boolean mDefault;

    static LocalPaymentNonce fromJSON(JSONObject inputJson) throws JSONException {
        JSONObject json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        JSONObject details = json.getJSONObject(DETAILS_KEY);
        String nonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        String description = json.getString(DESCRIPTION_KEY);
        boolean isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
        String email = Json.optString(details, EMAIL_KEY, null);
        String clientMetadataId = Json.optString(details, CLIENT_METADATA_ID_KEY, null);
        String type = Json.optString(json, TYPE_KEY, "PayPalAccount");

        PostalAddress billingAddress = null;
        PostalAddress shippingAddress = null;
        String givenName = null;
        String surname = null;
        String phone = null;
        String payerId = null;
        try {
            JSONObject payerInfo = details.getJSONObject(PAYER_INFO_KEY);

            JSONObject billingAddressJson;
            if (payerInfo.has(ACCOUNT_ADDRESS_KEY)) {
                billingAddressJson = payerInfo.optJSONObject(ACCOUNT_ADDRESS_KEY);
            } else {
                billingAddressJson = payerInfo.optJSONObject(BILLING_ADDRESS_KEY);
            }

            JSONObject shippingAddressJson = payerInfo.optJSONObject(SHIPPING_ADDRESS_KEY);

            billingAddress = PostalAddressParser.fromJson(billingAddressJson);
            shippingAddress = PostalAddressParser.fromJson(shippingAddressJson);

            givenName = Json.optString(payerInfo, FIRST_NAME_KEY, "");
            surname = Json.optString(payerInfo, LAST_NAME_KEY, "");
            phone = Json.optString(payerInfo, PHONE_KEY, "");
            payerId = Json.optString(payerInfo, PAYER_ID_KEY, "");

            if(email == null) {
                email = Json.optString(payerInfo, EMAIL_KEY, null);
            }
        } catch (JSONException e) {
            billingAddress = new PostalAddress();
            shippingAddress = new PostalAddress();
        }

        return new LocalPaymentNonce(clientMetadataId, billingAddress, shippingAddress, givenName, surname, phone, email, payerId, type, nonce, description, isDefault);
    }

    private LocalPaymentNonce(String clientMetadataId, PostalAddress billingAddress, PostalAddress shippingAddress, String givenName, String surname, String phone, String email, String payerId, String type, String nonce, String description, boolean isDefault) {
        super(nonce, description, isDefault, "TODO", PaymentMethodType.LOCAL_PAYMENT);
        mClientMetadataId = clientMetadataId;
        mBillingAddress = billingAddress;
        mShippingAddress = shippingAddress;
        mGivenName = givenName;
        mSurname = surname;
        mPhone = phone;
        mEmail = email;
        mPayerId = payerId;
        mType = type;
        mNonce = nonce;
        mDescription = description;
        mDefault = isDefault;
    }

    /**
     * @return The email address associated with this local payment
     */
    public String getEmail() {
        return mEmail;
    }

    @Override
    public String getString() {
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
     * @return The type of this {@link BraintreeNonce}
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
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
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
        super(in);
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
