package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.Json;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.models.BinData.BIN_DATA_KEY;

/**
 * {@link PaymentMethodNonce} representing a Visa Checkout card.
 * @see PaymentMethodNonce
 */
public class VisaCheckoutNonce extends PaymentMethodNonce implements Parcelable {

    protected static final String TYPE = "VisaCheckoutCard";
    protected static final String API_RESOURCE_KEY = "visaCheckoutCards";

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

    /**
     * Convert an API response to a {@link VisaCheckoutNonce}.
     *
     * @param json Raw JSON response from Braintree of a {@link VisaCheckoutNonce}.
     * @return {@link VisaCheckoutNonce}.
     * @throws JSONException when parsing the response fails.
     */
    public static VisaCheckoutNonce fromJson(String json) throws JSONException {
        VisaCheckoutNonce visaCheckoutNonce = new VisaCheckoutNonce();
        visaCheckoutNonce.fromJson(PaymentMethodNonce.getJsonObjectForType(API_RESOURCE_KEY, json));
        return visaCheckoutNonce;
    }

    @Override
    protected void fromJson(JSONObject json) throws JSONException {
        super.fromJson(json);

        mBinData = BinData.fromJson(json.optJSONObject(BIN_DATA_KEY));
        JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
        mLastTwo = details.getString(LAST_TWO_KEY);
        mCardType = details.getString(CARD_TYPE_KEY);
        mBillingAddress = VisaCheckoutAddress.fromJson(json.getJSONObject(BILLING_ADDRESS_KEY));
        mShippingAddress = VisaCheckoutAddress.fromJson(json.getJSONObject(SHIPPING_ADDRESS_KEY));
        mUserData = VisaCheckoutUserData.fromJson(json.getJSONObject(USER_DATA_KEY));
        mCallId = Json.optString(json, CALL_ID_KEY, "");
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
     * @return The Call ID from the {@link com.visa.checkout.VisaPaymentSummary}.
     */
    public String getCallId() {
        return mCallId;
    }

    @Override
    public String getTypeLabel() {
        return "Visa Checkout";
    }

    /**
     * @return The BIN data for the card number associated with {@link VisaCheckoutNonce} or
     * {@code null}
     */
    public BinData getBinData() {
        return mBinData;
    }

    public VisaCheckoutNonce() {}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mLastTwo);
        dest.writeString(mCardType);
        dest.writeParcelable(mBillingAddress, flags);
        dest.writeParcelable(mShippingAddress, flags);
        dest.writeParcelable(mUserData, flags);
        dest.writeString(mCallId);
        dest.writeParcelable(mBinData, flags);
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
