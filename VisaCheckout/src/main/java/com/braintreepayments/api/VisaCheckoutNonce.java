package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.BinData.BIN_DATA_KEY;


/**
 * {@link BraintreeNonce} representing a Visa Checkout card.
 * @see BraintreeNonce
 */
public class VisaCheckoutNonce extends BraintreeNonce {

    static final String TYPE = "VisaCheckoutCard";
    private static final String API_RESOURCE_KEY = "visaCheckoutCards";

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";
    private static final String DESCRIPTION_KEY = "description";

    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";
    private static final String LAST_TWO_KEY = "lastTwo";
    private static final String BILLING_ADDRESS_KEY = "billingAddress";
    private static final String SHIPPING_ADDRESS_KEY = "shippingAddress";
    private static final String USER_DATA_KEY = "userData";
    private static final String CALL_ID_KEY = "callId";

    private final String mLastTwo;
    private final String mCardType;
    private final VisaCheckoutAddress mBillingAddress;
    private final VisaCheckoutAddress mShippingAddress;
    private final VisaCheckoutUserData mUserData;
    private final String mCallId;
    private final BinData mBinData;

    private final String mNonce;
    private final String mDescription;
    private final boolean mDefault;

    static VisaCheckoutNonce fromJSON(JSONObject inputJson) throws JSONException {
        JSONObject json;
        if (inputJson.has(API_RESOURCE_KEY)) {
            json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        } else {
            json = inputJson;
        }

        JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
        String lastTwo = details.getString(LAST_TWO_KEY);
        String cardType = details.getString(CARD_TYPE_KEY);
        VisaCheckoutAddress billingAddress = VisaCheckoutAddress.fromJson(json.optJSONObject(BILLING_ADDRESS_KEY));
        VisaCheckoutAddress shippingAddress = VisaCheckoutAddress.fromJson(json.optJSONObject(SHIPPING_ADDRESS_KEY));
        VisaCheckoutUserData userData = VisaCheckoutUserData.fromJson(json.optJSONObject(USER_DATA_KEY));
        String callId = Json.optString(json, CALL_ID_KEY, "");
        BinData binData = BinData.fromJson(json.optJSONObject(BIN_DATA_KEY));

        String nonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        String description = json.getString(DESCRIPTION_KEY);
        boolean isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

        return new VisaCheckoutNonce(lastTwo, cardType, billingAddress, shippingAddress, userData, callId, binData, nonce, description, isDefault);
    }

    private VisaCheckoutNonce(String lastTwo, String cardType, VisaCheckoutAddress billingAddress, VisaCheckoutAddress shippingAddress, VisaCheckoutUserData userData, String callId, BinData binData, String nonce, String description, boolean isDefault) {
        super(nonce, description, isDefault, "TODO", PaymentMethodType.VISA_CHECKOUT);
        mLastTwo = lastTwo;
        mCardType = cardType;
        mBillingAddress = billingAddress;
        mShippingAddress = shippingAddress;
        mUserData = userData;
        mCallId = callId;
        mBinData = binData;
        mNonce = nonce;
        mDescription = description;
        mDefault = isDefault;
    }

    /**
     * @return Last two digits of the user's underlying card, intended for display purposes.
     */
    public String getLastTwo() {
        return mLastTwo;
    }

    /**
     * @return Type of this card (e.g. Visa, MasterCard, American Express)
     */
    public String getCardType() {
        return mCardType;
    }

    /**
     * @return The user's billing address.
     */
    public VisaCheckoutAddress getBillingAddress() {
        return mBillingAddress;
    }

    /**
     * @return The user's shipping address.
     */
    public VisaCheckoutAddress getShippingAddress() {
        return mShippingAddress;
    }

    /**
     * @return The user's data.
     */
    public VisaCheckoutUserData getUserData() {
        return mUserData;
    }

    /**
     * @return The Call ID from the VisaPaymentSummary.
     */
    public String getCallId() {
        return mCallId;
    }

    @Override
    public String getString() {
        return mNonce;
    }

    @Override
    public String getDescription() {
        return mDescription;
    }

    @Override
    public boolean isDefault() {
        return mDefault;
    }

    @Override
    public String getTypeLabel() {
        return "Visa Checkout";
    }

    /**
     * @return The BIN data for the card number associated with {@link VisaCheckoutNonce}
     */
    public BinData getBinData() {
        return mBinData;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mLastTwo);
        dest.writeString(mCardType);
        dest.writeParcelable(mBillingAddress, flags);
        dest.writeParcelable(mShippingAddress, flags);
        dest.writeParcelable(mUserData, flags);
        dest.writeString(mCallId);
        dest.writeParcelable(mBinData, flags);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeByte(mDefault ? (byte) 1 : (byte) 0);
    }

    protected VisaCheckoutNonce(Parcel in) {
        super(in);
        mLastTwo = in.readString();
        mCardType = in.readString();
        mBillingAddress = in.readParcelable(VisaCheckoutAddress.class.getClassLoader());
        mShippingAddress = in.readParcelable(VisaCheckoutAddress.class.getClassLoader());
        mUserData = in.readParcelable(VisaCheckoutUserData.class.getClassLoader());
        mCallId = in.readString();
        mBinData = in.readParcelable(BinData.class.getClassLoader());
        mNonce = in.readString();
        mDescription = in.readString();
        mDefault = in.readByte() > 0;
    }

    public static final Creator<VisaCheckoutNonce> CREATOR =
            new Creator<VisaCheckoutNonce>() {
                @Override
                public VisaCheckoutNonce createFromParcel(Parcel in) {
                    return new VisaCheckoutNonce(in);
                }

                @Override
                public VisaCheckoutNonce[] newArray(int size) {
                    return new VisaCheckoutNonce[size];
                }
            };
}
