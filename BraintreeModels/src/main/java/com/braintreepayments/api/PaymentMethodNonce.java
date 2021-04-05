package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.CallSuper;

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

    static final String DATA_KEY = "data";
    static final String TOKEN_KEY = "token";

    protected String mNonce;
    protected String mDescription;
    protected boolean mDefault;

    protected String mType;

    PaymentMethodNonce(String jsonString) throws JSONException {
        this(new JSONObject(jsonString));
    }

    PaymentMethodNonce(JSONObject json) throws JSONException {
        mNonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        mDescription = json.getString(DESCRIPTION_KEY);
        mDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
        mType = json.getString(PAYMENT_METHOD_TYPE_KEY);
    }

    static JSONObject getJsonObjectForType(String apiResourceKey, JSONObject json) throws JSONException {
        return json.getJSONArray(apiResourceKey).getJSONObject(0);
    }

    @CallSuper
    void fromJson(JSONObject json) throws JSONException {
        mNonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        mDescription = json.getString(DESCRIPTION_KEY);
        mDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
    }

    /**
     * @return The nonce generated for this payment method by the Braintree gateway. The nonce will
     *          represent this PaymentMethod for the purposes of creating transactions and other monetary
     *          actions.
     */
    public String getNonce() {
        return mNonce;
    }

    /**
     * @return The description of this PaymentMethod for displaying to a customer, e.g. 'Visa ending in...'
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * @return {@code true} if this payment method is the default for the current customer, {@code false} otherwise
     */
    public boolean isDefault() {
        return mDefault;
    }

    /**
     * @return The type of this PaymentMethod for displaying to a customer, e.g. 'Visa'. Can be used
     *          for displaying appropriate logos, etc.
     */
    public String getTypeLabel() {
        // TODO: introspect json and return a type label (e.g. Venmo, PayPal, Mastercard)
        return null;
    }

    //    /**
//     * Parses a {@link PaymentMethodNonce} from json.
//     *
//     * @param json {@link String} representation of a {@link PaymentMethodNonce}.
//     * @param type The {@link String} type of the {@link PaymentMethodNonce}.
//     * @return {@link PaymentMethodNonce}
//     * @throws JSONException if parsing fails
//     */
//    @Nullable
//    static PaymentMethodNonce parsePaymentMethodNonces(String json, String type) throws JSONException {
//        return parsePaymentMethodNonces(new JSONObject(json), type);
//    }
//
//    /**
//     * Parses a {@link PaymentMethodNonce} from json.
//     *
//     * @param json {@link JSONObject} representation of a {@link PaymentMethodNonce}.
//     * @param type The {@link String} type of the {@link PaymentMethodNonce}.
//     * @return {@link PaymentMethodNonce}
//     * @throws JSONException if parsing fails
//     */
//    @Nullable
//    static PaymentMethodNonce parsePaymentMethodNonces(JSONObject json, String type) throws JSONException {
//        switch (type) {
//            case VenmoAccountNonce.TYPE:
//                if (json.has(VenmoAccountNonce.API_RESOURCE_KEY)) {
//                    return VenmoAccountNonce.fromJson(json.toString());
//                } else {
//                    VenmoAccountNonce venmoAccountNonce = new VenmoAccountNonce();
//                    venmoAccountNonce.fromJson(json);
//                    return venmoAccountNonce;
//                }
//            case VisaCheckoutNonce.TYPE:
//                if (json.has(VisaCheckoutNonce.API_RESOURCE_KEY)) {
//                    return VisaCheckoutNonce.fromJson(json.toString());
//                } else {
//                    VisaCheckoutNonce visaCheckoutNonce = new VisaCheckoutNonce();
//                    visaCheckoutNonce.fromJson(json);
//                    return visaCheckoutNonce;
//                }
//            default:
//                return null;
//        }
//    }

    PaymentMethodNonce() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeByte(mDefault ? (byte) 1 : (byte) 0);
        dest.writeString(mType);
    }

    protected PaymentMethodNonce(Parcel in) {
        mNonce = in.readString();
        mDescription = in.readString();
        mDefault = in.readByte() > 0;
        mType = in.readString();
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
}
