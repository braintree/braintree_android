package com.braintreepayments.api;

import android.os.Parcel;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link PaymentMethodNonce} representing a PayPal account.
 *
 * @see PaymentMethodNonce
 */
public class PayPalAccountNonce extends PaymentMethodNonce {

    static final String API_RESOURCE_KEY = "paypalAccounts";
    private static final String PAYMENT_METHOD_DATA_KEY = "paymentMethodData";
    private static final String TOKENIZATION_DATA_KEY = "tokenizationData";
    private static final String TOKEN_KEY = "token";

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";

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

    private final String clientMetadataId;
    private final PostalAddress billingAddress;
    private final PostalAddress shippingAddress;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String email;
    private final String payerId;
    private final PayPalCreditFinancing creditFinancing;
    private final String authenticateUrl;

    static PayPalAccountNonce fromJSON(JSONObject inputJson) throws JSONException {
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

        String nonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        boolean isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

        String authenticateUrl = Json.optString(json, "authenticateUrl", null);

        JSONObject details = json.getJSONObject(DETAILS_KEY);
        String email = Json.optString(details, EMAIL_KEY, null);
        String clientMetadataId = Json.optString(details, CLIENT_METADATA_ID_KEY, null);

        PayPalCreditFinancing payPalCreditFinancing = null;
        PostalAddress shippingAddress;
        PostalAddress billingAddress;
        String firstName = null;
        String lastName = null;
        String phone = null;
        String payerId = null;
        try {
            if (details.has(CREDIT_FINANCING_KEY)) {
                JSONObject creditFinancing = details.getJSONObject(CREDIT_FINANCING_KEY);
                payPalCreditFinancing = PayPalCreditFinancing.fromJson(creditFinancing);
            }

            JSONObject payerInfo = details.getJSONObject(PAYER_INFO_KEY);

            JSONObject billingAddressJson = payerInfo.optJSONObject(BILLING_ADDRESS_KEY);
            if (payerInfo.has(ACCOUNT_ADDRESS_KEY)) {
                billingAddressJson = payerInfo.optJSONObject(ACCOUNT_ADDRESS_KEY);
            }

            shippingAddress = PostalAddressParser.fromJson(payerInfo.optJSONObject(SHIPPING_ADDRESS_KEY));
            billingAddress = PostalAddressParser.fromJson(billingAddressJson);
            firstName = Json.optString(payerInfo, FIRST_NAME_KEY, "");
            lastName = Json.optString(payerInfo, LAST_NAME_KEY, "");
            phone = Json.optString(payerInfo, PHONE_KEY, "");
            payerId = Json.optString(payerInfo, PAYER_ID_KEY, "");

            if (email == null) {
                email = Json.optString(payerInfo, EMAIL_KEY, null);
            }
        } catch (JSONException e) {
            billingAddress = new PostalAddress();
            shippingAddress = new PostalAddress();
        }

        // shipping address should be overriden when 'PAYMENT_METHOD_DATA_KEY' is present at the top-level;
        // this occurs when parsing a GooglePay PayPal Account Nonce
        if (getShippingAddressFromTopLevel) {
            JSONObject shippingAddressJson = json.optJSONObject(SHIPPING_ADDRESS_KEY);
            if (shippingAddressJson != null) {
                shippingAddress = PostalAddressParser.fromJson(shippingAddressJson);
            }
        }

        return new PayPalAccountNonce(clientMetadataId, billingAddress, shippingAddress, firstName, lastName, phone, email, payerId, payPalCreditFinancing, authenticateUrl, nonce, isDefault);
    }

    private PayPalAccountNonce(String clientMetadataId, PostalAddress billingAddress, PostalAddress shippingAddress, String firstName, String lastName, String phone, String email, String payerId, PayPalCreditFinancing creditFinancing, String authenticateUrl, String nonce, boolean isDefault) {
        super(nonce, isDefault);
        this.clientMetadataId = clientMetadataId;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.payerId = payerId;
        this.creditFinancing = creditFinancing;
        this.authenticateUrl = authenticateUrl;
    }

    /**
     * @return The email address associated with this PayPal account
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return The billing address of the user if requested with additional scopes.
     */
    public PostalAddress getBillingAddress() {
        return billingAddress;
    }

    /**
     * @return The shipping address of the user provided by checkout flows.
     */
    public PostalAddress getShippingAddress() {
        return shippingAddress;
    }

    /**
     * @return The first name associated with the PayPal account.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * @return The last name associated with the PayPal account.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * @return The phone number associated with the PayPal account.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * @return The ClientMetadataId associated with this transaction.
     */
    public String getClientMetadataId() {
        return clientMetadataId;
    }

    /**
     * @return The Payer ID provided in checkout flows.
     */
    public String getPayerId() {
        return payerId;
    }

    /**
     * @return The credit financing details. This property will only be present when the customer pays with PayPal Credit.
     */
    @Nullable
    public PayPalCreditFinancing getCreditFinancing() {
        return creditFinancing;
    }

    /**
     * @return The URL used to authenticate the customer during two-factor authentication flows.
     * This property will only be present if two-factor authentication is required.
     */
    @Nullable
    public String getAuthenticateUrl() {
        return authenticateUrl;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(clientMetadataId);
        dest.writeParcelable(billingAddress, flags);
        dest.writeParcelable(shippingAddress, flags);
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(email);
        dest.writeString(phone);
        dest.writeString(payerId);
        dest.writeParcelable(creditFinancing, flags);
        dest.writeString(authenticateUrl);
    }

    private PayPalAccountNonce(Parcel in) {
        super(in);
        clientMetadataId = in.readString();
        billingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        shippingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        firstName = in.readString();
        lastName = in.readString();
        email = in.readString();
        phone = in.readString();
        payerId = in.readString();
        creditFinancing = in.readParcelable(PayPalCreditFinancing.class.getClassLoader());
        authenticateUrl = in.readString();
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
