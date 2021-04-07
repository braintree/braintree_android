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

    protected String mNonce;
    protected String mDescription;
    protected boolean mDefault;

    protected String mTypeLabel;
    protected String mJsonString;

    protected @PaymentMethodType int mType;

    static BraintreeNonce fromJson(JSONObject inputJson) throws JSONException {
        return parseBraintreeNonce(inputJson);
    }

    private BraintreeNonce(String nonce, @PaymentMethodType int type, String description, JSONObject inputJson, String typeLabel, boolean isDefault) {
        mNonce = nonce;
        mType = type;
        mDescription = description;
        mJsonString = inputJson.toString();
        mTypeLabel = typeLabel;
        mDefault = isDefault;
    }

    private static BraintreeNonce parseBraintreeNonce(JSONObject json) throws JSONException {

        String typeString = json.getString(PAYMENT_METHOD_TYPE_KEY);
        @PaymentMethodType int type = paymentMethodTypeFromString(typeString);

        String nonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        String description = json.getString(DESCRIPTION_KEY);
        boolean isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

        String typeLabel;
        if (type == PaymentMethodType.CARD) {
            JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
            typeLabel = details.getString(CARD_TYPE_KEY);
        } else {
            typeLabel = displayNameFromPaymentMethodType(type);
        }
        return new BraintreeNonce(nonce, type, description, json, typeLabel, isDefault);
    }

    /**
     * @inheritDoc
     */
    public String getNonce() {
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

    protected BraintreeNonce(Parcel in) {
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
