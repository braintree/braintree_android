package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.BinData.BIN_DATA_KEY;

/**
 * {@link UntypedPaymentMethodNonce} representing a Visa Checkout card.
 * @see UntypedPaymentMethodNonce
 */
public class VisaCheckoutNonce implements PaymentMethodNonce {

    static final String TYPE = "VisaCheckoutCard";
    static final String API_RESOURCE_KEY = "visaCheckoutCards";

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

    private String mLastTwo;
    private String mCardType;
    private VisaCheckoutAddress mBillingAddress;
    private VisaCheckoutAddress mShippingAddress;
    private VisaCheckoutUserData mUserData;
    private String mCallId;
    private BinData mBinData;

    protected String mNonce;
    protected String mDescription;
    protected boolean mDefault;

    VisaCheckoutNonce(String jsonString) throws JSONException {
        this(new JSONObject(jsonString));
    }

    VisaCheckoutNonce(JSONObject inputJson) throws JSONException {
        JSONObject json;
        if (inputJson.has(API_RESOURCE_KEY)) {
            json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        } else {
            json = inputJson;
        }

        JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
        mLastTwo = details.getString(LAST_TWO_KEY);
        mCardType = details.getString(CARD_TYPE_KEY);
        mBillingAddress = VisaCheckoutAddress.fromJson(json.optJSONObject(BILLING_ADDRESS_KEY));
        mShippingAddress = VisaCheckoutAddress.fromJson(json.optJSONObject(SHIPPING_ADDRESS_KEY));
        mUserData = VisaCheckoutUserData.fromJson(json.optJSONObject(USER_DATA_KEY));
        mCallId = Json.optString(json, CALL_ID_KEY, "");
        mBinData = BinData.fromJson(json.optJSONObject(BIN_DATA_KEY));

        mNonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        mDescription = json.getString(DESCRIPTION_KEY);
        mDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);
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
    public String getNonce() {
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
