package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.PaymentMethodTypeUtils.paymentMethodTypeFromString;
import static com.braintreepayments.api.PaymentMethodTypeUtils.displayNameFromPaymentMethodType;

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
    private final String mJsonString;

    private @PaymentMethodType int mType;

    BraintreeNonce(JSONObject json) throws JSONException {
        String typeString = json.getString(PAYMENT_METHOD_TYPE_KEY);
        @PaymentMethodType int type = paymentMethodTypeFromString(typeString);

        mNonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        mDescription = json.getString(DESCRIPTION_KEY);
        mDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

        if (type == PaymentMethodType.CARD) {
            JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
            mTypeLabel = details.getString(CARD_TYPE_KEY);
        } else {
            mTypeLabel = displayNameFromPaymentMethodType(type);
        }

        // used when converting a BraintreeNonce into other 'typed' nonces
        mJsonString = json.toString();
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

    /**
     * @return type of payment method.
     */
    public int getType() {
        return mType;
    }

    String getJson() {
        return mJsonString;
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
        dest.writeString(mJsonString);
    }

    private BraintreeNonce(Parcel in) {
        mNonce = in.readString();
        mDescription = in.readString();
        mDefault = in.readByte() > 0;
        mType = in.readInt();
        mTypeLabel = in.readString();
        mJsonString = in.readString();
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
