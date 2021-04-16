package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.PaymentMethodTypeUtils.displayNameFromPaymentMethodType;
import static com.braintreepayments.api.PaymentMethodTypeUtils.paymentMethodTypeFromString;

/**
 * Base class representing a method of payment for a customer. {@link BraintreeNonce} represents the
 * common interface of all payment method nonces, and can be handled by a server interchangeably.
 */
public class BraintreeNonce implements PaymentMethodNonce, Parcelable {

    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";

    private static final String PAYMENT_METHOD_TYPE_KEY = "type";
    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";
    private static final String DESCRIPTION_KEY = "description";

    private final String mNonce;
    private final String mDescription;
    private final boolean mDefault;

    private final String mTypeLabel;

    private @PaymentMethodType final int mType;

    static BraintreeNonce fromJSON(JSONObject inputJson) throws JSONException {
        String typeString = inputJson.getString(PAYMENT_METHOD_TYPE_KEY);
        int type = paymentMethodTypeFromString(typeString);

        String nonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY);
        String description = inputJson.getString(DESCRIPTION_KEY);
        boolean isDefault = inputJson.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

        String typeLabel;
        if (type == PaymentMethodType.CARD) {
            JSONObject details = inputJson.getJSONObject(CARD_DETAILS_KEY);
            typeLabel = details.getString(CARD_TYPE_KEY);
        } else {
            typeLabel = displayNameFromPaymentMethodType(type);
        }

        return new BraintreeNonce(nonce, description, isDefault, typeLabel, type);
    }

    private BraintreeNonce(String nonce, String description, boolean isDefault, String typeLabel, @PaymentMethodType int type) {
        mNonce = nonce;
        mDescription = description;
        mDefault = isDefault;
        mTypeLabel = typeLabel;
        mType = type;
    }

    /**
     * @inheritDoc
     */
    public String getString() {
        return mNonce;
    }

    /**
     * @inheritDoc
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * @inheritDoc
     */
    public boolean isDefault() {
        return mDefault;
    }

    /**
     * @return The type of this PaymentMethod for displaying to a customer, e.g. 'Visa'. Can be used
     * for displaying appropriate logos, etc.
     */
    public String getTypeLabel() {
        return mTypeLabel;
    }

    int getType() {
        return mType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeByte(mDefault ? (byte) 1 : (byte) 0);
        dest.writeInt(mType);
        dest.writeString(mTypeLabel);
    }

    private BraintreeNonce(Parcel in) {
        mNonce = in.readString();
        mDescription = in.readString();
        mDefault = in.readByte() > 0;
        mType = in.readInt();
        mTypeLabel = in.readString();
    }

    public static final Creator<BraintreeNonce> CREATOR = new Creator<BraintreeNonce>() {
        @Override
        public BraintreeNonce createFromParcel(Parcel in) {
            return new BraintreeNonce(in);
        }

        @Override
        public BraintreeNonce[] newArray(int size) {
            return new BraintreeNonce[size];
        }
    };
}
