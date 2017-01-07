package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

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
    private static final String SHIPPING_ADDRESS = "shippingAddress";
    private static final String USER_DATA = "userData";

    private String mLastTwo;
    private String mCardType;
    private VisaCheckoutAddress mShippingAddress;
    private VisaCheckoutUserData mUserData;

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

        JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
        mLastTwo = details.getString(LAST_TWO_KEY);
        mCardType = details.getString(CARD_TYPE_KEY);

        mShippingAddress = new VisaCheckoutAddress(json.getJSONObject(SHIPPING_ADDRESS));
        mUserData = VisaCheckoutUserData.fromJson(json.getJSONObject(USER_DATA));
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

    @Override
    public String getTypeLabel() {
        return "Visa Checkout";
    }

    private VisaCheckoutNonce() {}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mLastTwo);
        dest.writeString(mCardType);
        dest.writeParcelable(mShippingAddress, flags);
        dest.writeParcelable(mUserData, flags);
    }

    protected VisaCheckoutNonce(Parcel in) {
        super(in);
        mLastTwo = in.readString();
        mCardType = in.readString();
        mShippingAddress = in.readParcelable(VisaCheckoutAddress.class.getClassLoader());
        mUserData = in.readParcelable(VisaCheckoutUserData.class.getClassLoader());
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
