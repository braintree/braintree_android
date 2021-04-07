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

    private static final String CARD_API_RESOURCE_KEY = "creditCards";
    private static final String PAYPAL_API_RESOURCE_KEY = "paypalAccounts";
    private static final String VENMO_API_RESOURCE_KEY = "venmoAccounts";
    private static final String VISA_CHECKOUT_API_RESOURCE_KEY = "visaCheckoutCards";
    private static final String GOOGLE_PAY_API_RESOURCE_KEY = "androidPayCards";

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

    protected @PaymentMethodType int mType;
    protected String mTypeLabel;
    protected String mJsonString;

    BraintreeNonce(String jsonString) throws JSONException {
        this(new JSONObject(jsonString));
    }

    BraintreeNonce(JSONObject inputJson) throws JSONException {
        mJsonString = inputJson.toString();

        boolean isGraphQL = false;
        boolean isGooglePay = false;

        String apiResourceKey = null;

        if (inputJson.has(DATA_KEY)) {
            mType = PaymentMethodType.CARD;
            isGraphQL = true;
        } else if (inputJson.has(CARD_API_RESOURCE_KEY)) {
            mType = PaymentMethodType.CARD;
            apiResourceKey = CARD_API_RESOURCE_KEY;
        } else if (inputJson.has(PAYPAL_API_RESOURCE_KEY)) {
            mType = PaymentMethodType.PAYPAL;
            apiResourceKey = PAYPAL_API_RESOURCE_KEY;
        } else if (inputJson.has(VENMO_API_RESOURCE_KEY)) {
            mType = PaymentMethodType.VENMO;
            apiResourceKey = VENMO_API_RESOURCE_KEY;
        } else if (inputJson.has(VISA_CHECKOUT_API_RESOURCE_KEY)) {
            mType = PaymentMethodType.VISA_CHECKOUT;
            apiResourceKey = VISA_CHECKOUT_API_RESOURCE_KEY;
        } else if (isGooglePay(inputJson)) {
            mType = PaymentMethodType.GOOGLE_PAY;
            isGooglePay = true;
        } else {
            String typeString = inputJson.getString(PAYMENT_METHOD_TYPE_KEY);
            mType = paymentMethodTypeFromString(typeString);
        }

        if (isGraphQL) {
            JSONObject data = inputJson.getJSONObject(DATA_KEY);

            if (data.has(GRAPHQL_TOKENIZE_CREDIT_CARD_KEY)) {
                JSONObject payload = data.getJSONObject(GRAPHQL_TOKENIZE_CREDIT_CARD_KEY);
                JSONObject creditCard = payload.getJSONObject(GRAPHQL_CREDIT_CARD_KEY);
                mTypeLabel = Json.optString(creditCard, GRAPHQL_BRAND_KEY, "Unknown");
                mNonce = payload.getString(TOKEN_KEY);
                String lastFour = Json.optString(creditCard, GRAPHQL_LAST_FOUR_KEY, "");
                String lastTwo = lastFour.length() < 4 ? "" : lastFour.substring(2);
                mDescription = TextUtils.isEmpty(lastTwo) ? "" : "ending in ••" + lastTwo;
                mDefault = false;
            }
        } else if (isGooglePay) {
            JSONObject token = new JSONObject(inputJson
                    .getJSONObject("paymentMethodData")
                    .getJSONObject("tokenizationData")
                    .getString("token"));

            JSONObject androidPayCardObject = new JSONObject(token.getJSONArray(GOOGLE_PAY_API_RESOURCE_KEY).get(0).toString());
            mNonce = androidPayCardObject.getString(PAYMENT_METHOD_NONCE_KEY);
            mDescription = androidPayCardObject.getString(DESCRIPTION_KEY);
            mDefault = androidPayCardObject.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
            mTypeLabel = "Google Pay";

        } else {
            JSONObject json;
            if (inputJson.has(apiResourceKey)) {
                json = inputJson.getJSONArray(apiResourceKey).getJSONObject(0);
            } else {
                json = inputJson;
            }

            mNonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
            mDescription = json.getString(DESCRIPTION_KEY);
            mDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

            if (mType == PaymentMethodType.CARD) {
                JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
                mTypeLabel = details.getString(CARD_TYPE_KEY);
            } else {
                mTypeLabel = displayNameFromPaymentMethodType(mType);
            }
        }
    }

    private static boolean isGooglePay(JSONObject inputJson) throws JSONException {
        if (inputJson.has("paymentMethodData")) {
            JSONObject paymentMethodData = inputJson.getJSONObject("paymentMethodData");
            if (paymentMethodData.has("tokenizationData")) {
                JSONObject tokenizationData = paymentMethodData.getJSONObject("tokenizationData");
                return tokenizationData.has("token");
            }
        }
        return false;
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
