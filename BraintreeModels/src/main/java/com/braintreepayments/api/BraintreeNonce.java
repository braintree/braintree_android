package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class representing a method of payment for a customer. {@link BraintreeNonce} represents the
 * common interface of all payment method nonces, and can be handled by a server interchangeably.
 */
public class BraintreeNonce implements PaymentMethodNonce, Parcelable {

    private static final String CARD_API_RESOURCE_KEY = "creditCards";
    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";
    static final String DATA_KEY = "data";
    static final String TOKEN_KEY = "token";

    private static final String GRAPHQL_TOKENIZE_CREDIT_CARD_KEY = "tokenizeCreditCard";
    private static final String GRAPHQL_CREDIT_CARD_KEY = "creditCard";
    private static final String GRAPHQL_BRAND_KEY = "brand";
    private static final String GRAPHQL_LAST_FOUR_KEY = "last4";

    private static final String PAYMENT_METHOD_TYPE_KEY = "type";
    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";
    private static final String DESCRIPTION_KEY = "description";

    protected String mNonce;
    protected String mDescription;
    protected boolean mDefault;

    protected String mType;
    protected String mTypeLabel;
    protected String mJsonString;

    BraintreeNonce(String jsonString) throws JSONException {
        this(new JSONObject(jsonString));
    }

    BraintreeNonce(JSONObject inputJson) throws JSONException {
        mType = inputJson.getString(PAYMENT_METHOD_TYPE_KEY);
        mJsonString = inputJson.toString();

        switch (mType) {
            case "CreditCard":
                if (inputJson.has(DATA_KEY)) {
                    JSONObject data = inputJson.getJSONObject(DATA_KEY);

                    if (data.has(GRAPHQL_TOKENIZE_CREDIT_CARD_KEY)) {
                        JSONObject payload = data.getJSONObject(GRAPHQL_TOKENIZE_CREDIT_CARD_KEY);
                        JSONObject creditCard = payload.getJSONObject(GRAPHQL_CREDIT_CARD_KEY);
                        mTypeLabel = Json.optString(creditCard, GRAPHQL_BRAND_KEY, "Unknown");
                        mNonce = payload.getString(TOKEN_KEY);
                        String mLastFour = Json.optString(creditCard, GRAPHQL_LAST_FOUR_KEY, "");
                        String mLastTwo = mLastFour.length() < 4 ? "" : mLastFour.substring(2);
                        mDescription = TextUtils.isEmpty(mLastTwo) ? "" : "ending in ••" + mLastTwo;
                        mDefault = false;
                    }
                } else {
                    JSONObject json;
                    if (inputJson.has(CARD_API_RESOURCE_KEY)) {
                        json = inputJson.getJSONArray(CARD_API_RESOURCE_KEY).getJSONObject(0);
                    } else {
                        json = inputJson;
                    }
                    mNonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
                    mDescription = json.getString(DESCRIPTION_KEY);
                    mDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

                    JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
                    mTypeLabel = details.getString(CARD_TYPE_KEY);
                }
                break;
            case "PayPalAccount":
                mNonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY);
                mDescription = inputJson.getString(DESCRIPTION_KEY);
                mDefault = inputJson.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
                mTypeLabel = "PayPal";
                break;
            case "VisaCheckoutCard":
                mNonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY);
                mDescription = inputJson.getString(DESCRIPTION_KEY);
                mDefault = inputJson.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
                mTypeLabel = "Visa Checkout";
                break;
            case "VenmoAccount":
                mNonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY);
                mDescription = inputJson.getString(DESCRIPTION_KEY);
                mDefault = inputJson.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
                mTypeLabel = "Venmo";
                break;
            default:
                mNonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY);
                mDescription = inputJson.getString(DESCRIPTION_KEY);
                mDefault = inputJson.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
                // TODO: consider throwing here for nonces that aren't supposed to be
                // parsed by payment methods client
                mTypeLabel = "Unknown";
                break;
        }
    }

    /** @inheritDoc */
    public String getNonce() {
        return mNonce;
    }

    /** @inheritDoc */
    public String getDescription() {
        return mDescription;
    }

    /** @inheritDoc */
    public boolean isDefault() {
        return mDefault;
    }

    /**
     * @return The type of this PaymentMethod for displaying to a customer, e.g. 'Visa'. Can be used
     *          for displaying appropriate logos, etc.
     */
    public String getTypeLabel() {
        return mTypeLabel;
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
        dest.writeString(mType);
        dest.writeString(mTypeLabel);
        dest.writeString(mJsonString);
    }

    protected BraintreeNonce(Parcel in) {
        mNonce = in.readString();
        mDescription = in.readString();
        mDefault = in.readByte() > 0;
        mType = in.readString();
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
