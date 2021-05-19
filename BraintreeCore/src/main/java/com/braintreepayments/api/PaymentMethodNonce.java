package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Base class representing a method of payment for a customer. {@link PaymentMethodNonce} represents the
 * common interface of all payment method nonces, and can be handled by a server interchangeably.
 */
public class PaymentMethodNonce implements Parcelable {

    private static final String PAYMENT_METHOD_TYPE_KEY = "type";
    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";
    private static final String DESCRIPTION_KEY = "description";

    private final String nonce;
    private final boolean isDefault;
    private final String typeLabel;
    private final String description;

    private @PaymentMethodType final int type;

    static PaymentMethodNonce fromJSON(JSONObject inputJson) throws JSONException {
        String typeString = inputJson.getString(PAYMENT_METHOD_TYPE_KEY);
        int type = paymentMethodTypeFromString(typeString);

        String nonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY);
        boolean isDefault = inputJson.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
        String description = inputJson.optString(DESCRIPTION_KEY);

        String typeLabel = "";

        JSONObject details = inputJson.optJSONObject("details");
        switch (typeString) {
            case "CreditCard":
                if (details != null) {
                    typeLabel = details.optString("cardType");
                }
                break;
            case "PayPalAccount":
                typeLabel = "PayPal";
                break;
            case "VisaCheckoutCard":
                typeLabel = "Visa Checkout";
                break;
            case "VenmoAccount":
                typeLabel = "Venmo";
                if (details != null) {
                    description = details.optString("username");
                }
                break;
            case "AndroidPayCard":
                typeLabel = "Google Pay";
                break;
            default:
                typeLabel = "Unknown";
                break;
        }
        return new PaymentMethodNonce(nonce, isDefault, type, typeLabel, description);
    }

    PaymentMethodNonce(String nonce, boolean isDefault, @PaymentMethodType int type, String typeLabel, String description) {
        this.nonce = nonce;
        this.isDefault = isDefault;
        this.type = type;
        this.typeLabel = typeLabel;
        this.description = description;
    }

    /**
     * @return The nonce generated for this payment method by the Braintree gateway. The nonce will
     *          represent this PaymentMethod for the purposes of creating transactions and other monetary
     *          actions.
     */
    public String getString() {
        return nonce;
    }

    /**
     * @return {@code true} if this payment method is the default for the current customer, {@code false} otherwise
     */
    public boolean isDefault() {
        return isDefault;
    }

    @PaymentMethodType int getType() {
        return type;
    }

    String getTypeLabel() {
        return typeLabel;
    }

    String getDescription() {
        return description;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nonce);
        dest.writeByte(isDefault ? (byte) 1 : (byte) 0);
        dest.writeInt(type);
        dest.writeString(typeLabel);
        dest.writeString(description);
    }

    protected PaymentMethodNonce(Parcel in) {
        nonce = in.readString();
        isDefault = in.readByte() > 0;
        type = in.readInt();
        typeLabel = in.readString();
        description = in.readString();
    }

    public static final Creator<PaymentMethodNonce> CREATOR = new Creator<PaymentMethodNonce>() {
        @Override
        public PaymentMethodNonce createFromParcel(Parcel in) {
            return new PaymentMethodNonce(in);
        }

        @Override
        public PaymentMethodNonce[] newArray(int size) {
            return new PaymentMethodNonce[size];
        }
    };

    private static @PaymentMethodType int paymentMethodTypeFromString(String typeString) {
        switch (typeString) {
            case "CreditCard":
                return PaymentMethodType.CARD;
            case "PayPalAccount":
                return PaymentMethodType.PAYPAL;
            case "VisaCheckoutCard":
                return PaymentMethodType.VISA_CHECKOUT;
            case "VenmoAccount":
                return PaymentMethodType.VENMO;
            case "AndroidPayCard":
                return PaymentMethodType.GOOGLE_PAY;
            default:
                return PaymentMethodType.UNKNOWN;
        }
    }
}
