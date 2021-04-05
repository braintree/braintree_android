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
    private static final String CARD_API_RESOURCE_KEY = "creditCards";
    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";

    private static final String GRAPHQL_TOKENIZE_CREDIT_CARD_KEY = "tokenizeCreditCard";
    private static final String GRAPHQL_CREDIT_CARD_KEY = "creditCard";
    private static final String GRAPHQL_BRAND_KEY = "brand";

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
    protected String mTypeLabel;

    PaymentMethodNonce(String jsonString) throws JSONException {
        this(new JSONObject(jsonString));
    }

    PaymentMethodNonce(JSONObject inputJson) throws JSONException {
        mNonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY);
        mDescription = inputJson.getString(DESCRIPTION_KEY);
        mDefault = inputJson.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
        mType = inputJson.getString(PAYMENT_METHOD_TYPE_KEY);

        switch (mType) {
            case "CreditCard":
                if (inputJson.has(DATA_KEY)) {
                    JSONObject data = inputJson.getJSONObject(DATA_KEY);

                    if (data.has(GRAPHQL_TOKENIZE_CREDIT_CARD_KEY)) {
                        JSONObject payload = data.getJSONObject(GRAPHQL_TOKENIZE_CREDIT_CARD_KEY);
                        JSONObject creditCard = payload.getJSONObject(GRAPHQL_CREDIT_CARD_KEY);
                        mTypeLabel = Json.optString(creditCard, GRAPHQL_BRAND_KEY, "Unknown");
                    }
                } else {
                    JSONObject json;
                    if (inputJson.has(CARD_API_RESOURCE_KEY)) {
                        json = inputJson.getJSONArray(CARD_API_RESOURCE_KEY).getJSONObject(0);
                    } else {
                        json = inputJson;
                    }

                    JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
                    mTypeLabel = details.getString(CARD_TYPE_KEY);
                }
                break;
            case "PayPalAccount":
                mTypeLabel = "PayPal";
                break;
            case "VisaCheckoutCard":
                mTypeLabel = "Visa Checkout";
                break;
            case "VenmoAccount":
                mTypeLabel = "Venmo";
                break;
            default:
                mTypeLabel = "Unknown";
                break;
        }
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
        return mTypeLabel;
    }

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
        dest.writeString(mTypeLabel);
    }

    protected PaymentMethodNonce(Parcel in) {
        mNonce = in.readString();
        mDescription = in.readString();
        mDefault = in.readByte() > 0;
        mType = in.readString();
        mTypeLabel = in.readString();
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
