package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link PaymentMethodNonce} representing a local payment.
 *
 * @see PaymentMethodNonce
 */
public class LocalPaymentNonce extends PaymentMethodNonce {

    private static final String API_RESOURCE_KEY = "paypalAccounts";

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";

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

    private final String clientMetadataId;
    private final PostalAddress billingAddress;
    private final PostalAddress shippingAddress;
    private final String givenName;
    private final String surname;
    private final String phone;
    private final String email;
    private final String payerId;
    private final String type;

    static LocalPaymentNonce fromJSON(JSONObject inputJson) throws JSONException {
        JSONObject json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        JSONObject details = json.getJSONObject(DETAILS_KEY);
        String nonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        boolean isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
        String email = Json.optString(details, EMAIL_KEY, null);
        String clientMetadataId = Json.optString(details, CLIENT_METADATA_ID_KEY, null);
        String type = Json.optString(json, TYPE_KEY, "PayPalAccount");

        PostalAddress billingAddress;
        PostalAddress shippingAddress;
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

        return new LocalPaymentNonce(clientMetadataId, billingAddress, shippingAddress, givenName, surname, phone, email, payerId, type, nonce, isDefault);
    }

    private LocalPaymentNonce(String clientMetadataId, PostalAddress billingAddress, PostalAddress shippingAddress, String givenName, String surname, String phone, String email, String payerId, String type, String nonce, boolean isDefault) {
        super(nonce, isDefault);
        this.clientMetadataId = clientMetadataId;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
        this.givenName = givenName;
        this.surname = surname;
        this.phone = phone;
        this.email = email;
        this.payerId = payerId;
        this.type = type;
    }

    /**
     * @return The email address associated with this local payment
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
     * @return The first name associated with the local payment.
     */
    public String getGivenName() {
        return givenName;
    }

    /**
     * @return The last name associated with the local payment.
     */
    public String getSurname() {
        return surname;
    }

    /**
     * @return The phone number associated with the local payment.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * @return The ClientMetadataId associated with this transaction.
     */
    public String getClientMetadataId(){
        return clientMetadataId;
    }

    /**
     * @return The Payer ID provided in local payment flows.
     */
    public String getPayerId(){
        return payerId;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(clientMetadataId);
        dest.writeParcelable(billingAddress, flags);
        dest.writeParcelable(shippingAddress, flags);
        dest.writeString(givenName);
        dest.writeString(surname);
        dest.writeString(email);
        dest.writeString(phone);
        dest.writeString(payerId);
        dest.writeString(type);
    }

    private LocalPaymentNonce(Parcel in) {
        super(in);
        clientMetadataId = in.readString();
        billingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        shippingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        givenName = in.readString();
        surname = in.readString();
        email = in.readString();
        phone = in.readString();
        payerId = in.readString();
        type = in.readString();
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
