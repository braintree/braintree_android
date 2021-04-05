package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link UntypedPaymentMethodNonce} representing a PayPal account.
 *
 * @see UntypedPaymentMethodNonce
 */
public class PayPalAccountNonce implements PaymentMethodNonce, Parcelable {

    static final String TYPE = "PayPalAccount";
    static final String API_RESOURCE_KEY = "paypalAccounts";
    static final String PAYMENT_METHOD_DATA_KEY = "paymentMethodData";
    static final String TOKENIZATION_DATA_KEY = "tokenizationData";
    static final String TOKEN_KEY = "token";

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";
    private static final String DESCRIPTION_KEY = "description";

    private static final String CREDIT_FINANCING_KEY = "creditFinancingOffered";
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

    private String mClientMetadataId;
    private PostalAddress mBillingAddress;
    private PostalAddress mShippingAddress;
    private String mFirstName;
    private String mLastName;
    private String mPhone;
    private String mEmail;
    private String mPayerId;
    private PayPalCreditFinancing mCreditFinancing;
    private String mAuthenticateUrl;

    protected String mNonce;
    protected String mDescription;
    protected boolean mDefault;

    PayPalAccountNonce(String jsonString) throws JSONException {
        this(new JSONObject(jsonString));
    }

    PayPalAccountNonce(JSONObject inputJson) throws JSONException {
        boolean getShippingAddressFromTopLevel = false;

        JSONObject json;
        if (inputJson.has(API_RESOURCE_KEY)) {
            json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        } else if (inputJson.has(PAYMENT_METHOD_DATA_KEY)) {
            getShippingAddressFromTopLevel = true;
            JSONObject tokenObj = new JSONObject(inputJson
                    .getJSONObject(PayPalAccountNonce.PAYMENT_METHOD_DATA_KEY)
                    .getJSONObject(PayPalAccountNonce.TOKENIZATION_DATA_KEY)
                    .getString(PayPalAccountNonce.TOKEN_KEY));
            json = tokenObj.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        } else {
            json = inputJson;
        }

        mNonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        mDescription = json.getString(DESCRIPTION_KEY);
        mDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

        mAuthenticateUrl = Json.optString(json, "authenticateUrl", null);

        JSONObject details = json.getJSONObject(DETAILS_KEY);
        mEmail = Json.optString(details, EMAIL_KEY, null);
        mClientMetadataId = Json.optString(details, CLIENT_METADATA_ID_KEY, null);

        try {
            if (details.has(CREDIT_FINANCING_KEY)) {
                JSONObject creditFinancing = details.getJSONObject(CREDIT_FINANCING_KEY);
                mCreditFinancing = PayPalCreditFinancing.fromJson(creditFinancing);
            }

            JSONObject payerInfo = details.getJSONObject(PAYER_INFO_KEY);

            JSONObject billingAddress = payerInfo.optJSONObject(BILLING_ADDRESS_KEY);
            if (payerInfo.has(ACCOUNT_ADDRESS_KEY)) {
                billingAddress = payerInfo.optJSONObject(ACCOUNT_ADDRESS_KEY);
            }

            mShippingAddress = PostalAddressParser.fromJson(payerInfo.optJSONObject(SHIPPING_ADDRESS_KEY));
            mBillingAddress = PostalAddressParser.fromJson(billingAddress);
            mFirstName = Json.optString(payerInfo, FIRST_NAME_KEY, "");
            mLastName = Json.optString(payerInfo, LAST_NAME_KEY, "");
            mPhone = Json.optString(payerInfo, PHONE_KEY, "");
            mPayerId = Json.optString(payerInfo, PAYER_ID_KEY, "");

            if (mEmail == null) {
                mEmail = Json.optString(payerInfo, EMAIL_KEY, null);
            }
        } catch (JSONException e) {
            mBillingAddress = new PostalAddress();
            mShippingAddress = new PostalAddress();
        }

        // TODO: this boolean exists to replicate existing functionality where shipping address
        // gets overriden when 'PAYMENT_METHOD_DATA_KEY' is present at the top-level
        if (getShippingAddressFromTopLevel) {
            JSONObject shippingAddress = json.optJSONObject(SHIPPING_ADDRESS_KEY);
            if (shippingAddress != null) {
                mShippingAddress = PostalAddressParser.fromJson(shippingAddress);
            }
        }
    }

    /**
     * @return The email address associated with this PayPal account
     */
    public String getEmail() {
        return mEmail;
    }

    @Override
    public String getNonce() {
        return mNonce;
    }

    /**
     * @return The description of this PayPal account for displaying to a customer, either email or
     * 'PayPal'
     */
    @Override
    public String getDescription() {
        if (TextUtils.equals(mDescription, "PayPal") && !TextUtils.isEmpty(getEmail())) {
            return getEmail();
        } else {
            return mDescription;
        }
    }

    @Override
    public boolean isDefault() {
        return mDefault;
    }

    /**
     * @return The type of this {@link UntypedPaymentMethodNonce} (always "PayPal")
     */
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
    public String getClientMetadataId() {
        return mClientMetadataId;
    }

    /**
     * @return The Payer ID provided in checkout flows.
     */
    public String getPayerId() {
        return mPayerId;
    }

    /**
     * @return The credit financing details. This property will only be present when the customer pays with PayPal Credit.
     */
    @Nullable
    public PayPalCreditFinancing getCreditFinancing() {
        return mCreditFinancing;
    }

    /**
     * @return The URL used to authenticate the customer during two-factor authentication flows.
     * This property will only be present if two-factor authentication is required.
     */
    @Nullable
    public String getAuthenticateUrl() {
        return mAuthenticateUrl;
    }

    PayPalAccountNonce() {
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
        dest.writeString(mFirstName);
        dest.writeString(mLastName);
        dest.writeString(mEmail);
        dest.writeString(mPhone);
        dest.writeString(mPayerId);
        dest.writeParcelable(mCreditFinancing, flags);
        dest.writeString(mAuthenticateUrl);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeByte(mDefault ? (byte) 1 : (byte) 0);
    }

    private PayPalAccountNonce(Parcel in) {
        mClientMetadataId = in.readString();
        mBillingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        mShippingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        mFirstName = in.readString();
        mLastName = in.readString();
        mEmail = in.readString();
        mPhone = in.readString();
        mPayerId = in.readString();
        mCreditFinancing = in.readParcelable(PayPalCreditFinancing.class.getClassLoader());
        mAuthenticateUrl = in.readString();
        mNonce = in.readString();
        mDescription = in.readString();
        mDefault = in.readByte() > 0;
    }

    public static final Creator<PayPalAccountNonce> CREATOR = new Creator<PayPalAccountNonce>() {
        public PayPalAccountNonce createFromParcel(Parcel source) {
            return new PayPalAccountNonce(source);
        }

        public PayPalAccountNonce[] newArray(int size) {
            return new PayPalAccountNonce[size];
        }
    };
}
