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

    private final String nonce;
    private final boolean isDefault;

    private @PaymentMethodType final int mType;

    static PaymentMethodNonce fromJSON(JSONObject inputJson) throws JSONException {
        String typeString = inputJson.getString(PAYMENT_METHOD_TYPE_KEY);
        int type = paymentMethodTypeFromString(typeString);

        String nonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY);
        boolean isDefault = inputJson.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

        return new PaymentMethodNonce(nonce, isDefault, type);
    }

    PaymentMethodNonce(String nonce, boolean isDefault, @PaymentMethodType int type) {
        this.nonce = nonce;
        this.isDefault = isDefault;
        mType = type;
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
        return mType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nonce);
        dest.writeByte(isDefault ? (byte) 1 : (byte) 0);
        dest.writeInt(mType);
    }

    protected PaymentMethodNonce(Parcel in) {
        nonce = in.readString();
        isDefault = in.readByte() > 0;
        mType = in.readInt();
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
