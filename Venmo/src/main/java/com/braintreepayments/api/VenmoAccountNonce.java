package com.braintreepayments.api;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link PaymentMethodNonce} representing a {@link VenmoAccountNonce}
 *
 * @see PaymentMethodNonce
 */
public class VenmoAccountNonce extends PaymentMethodNonce {

    private static final String API_RESOURCE_KEY = "venmoAccounts";
    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";

    private static final String VENMO_DETAILS_KEY = "details";
    private static final String VENMO_USERNAME_KEY = "username";

    private static final String VENMO_PAYMENT_METHOD_ID_KEY = "paymentMethodId";
    private static final String VENMO_PAYER_INFO_KEY = "payerInfo";
    private static final String VENMO_EMAIL_KEY = "email";
    private static final String VENMO_EXTERNAL_ID_KEY = "externalId";
    private static final String VENMO_FIRST_NAME_KEY = "firstName";
    private static final String VENMO_LAST_NAME_KEY = "lastName";
    private static final String VENMO_PHONE_NUMBER_KEY = "phoneNumber";
    private static final String VENMO_PAYMENT_METHOD_USERNAME_KEY = "userName";

    private String email;
    private String externalId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String username;

    static VenmoAccountNonce fromJSON(JSONObject inputJson) throws JSONException {
        JSONObject json;
        if (inputJson.has(API_RESOURCE_KEY)) {
            json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        } else {
            json = inputJson;
        }

        String nonce;
        boolean isDefault;
        String username;

        if (json.has(VENMO_PAYMENT_METHOD_ID_KEY)) {
            isDefault = false;
            nonce = json.getString(VENMO_PAYMENT_METHOD_ID_KEY);
            username = json.getString(VENMO_PAYMENT_METHOD_USERNAME_KEY);
        } else {
            nonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
            isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

            JSONObject details = json.getJSONObject(VENMO_DETAILS_KEY);
            username = details.getString(VENMO_USERNAME_KEY);
        }

        return new VenmoAccountNonce(nonce, username, isDefault, json);
    }

    VenmoAccountNonce(String nonce, String username, boolean isDefault, JSONObject json) throws JSONException {
        super(nonce, isDefault);
        this.username = username;

        if (json.has(VENMO_PAYER_INFO_KEY)) {
            JSONObject payerInfo = json.getJSONObject(VENMO_PAYER_INFO_KEY);
            this.email = payerInfo.optString(VENMO_EMAIL_KEY);
            this.externalId = payerInfo.optString(VENMO_EXTERNAL_ID_KEY);
            this.firstName = payerInfo.optString(VENMO_FIRST_NAME_KEY);
            this.lastName = payerInfo.optString(VENMO_LAST_NAME_KEY);
            this.phoneNumber = payerInfo.optString(VENMO_PHONE_NUMBER_KEY);
        }
    }

    VenmoAccountNonce(String nonce, String username, boolean isDefault) {
        super(nonce, isDefault);
        this.username = username;
    }

    /**
     * @return the Venmo user's email
     */
    @Nullable
    public String getEmail() {
        return email;
    }

    /**
     * @return the Venmo user's external ID
     */
    @Nullable
    public String getExternalId() {
        return externalId;
    }

    /**
     * @return the Venmo user's first name
     */
    @Nullable
    public String getFirstName() {
        return firstName;
    }

    /**
     * @return the Venmo user's last name
     */
    @Nullable
    public String getLastName() {
        return lastName;
    }

    /**
     * @return the Venmo user's phone number
     */
    @Nullable
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * @return the Venmo username
     */
    @NonNull
    public String getUsername() {
        return username;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(email);
        dest.writeString(externalId);
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(phoneNumber);
        dest.writeString(username);
    }

    private VenmoAccountNonce(Parcel in) {
        super(in);
        email = in.readString();
        externalId = in.readString();
        firstName = in.readString();
        lastName = in.readString();
        phoneNumber = in.readString();
        username = in.readString();
    }

    public static final Creator<VenmoAccountNonce> CREATOR = new Creator<VenmoAccountNonce>() {
        @Override
        public VenmoAccountNonce createFromParcel(Parcel in) {
            return new VenmoAccountNonce(in);
        }

        @Override
        public VenmoAccountNonce[] newArray(int size) {
            return new VenmoAccountNonce[size];
        }
    };
}
