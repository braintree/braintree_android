package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

public class VisaCheckoutPaymentMethodNonce extends PaymentMethodNonce implements Parcelable {

    public static final String TYPE = "VisaCheckoutCard";
    protected static final String API_RESOURCE_KEY = "visaCheckoutCards";

    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";
    private static final String LAST_TWO_KEY = "lastTwo";
    private static final String SHIPPING_ADDRESS = "shippingAddress";
    private static final String USER_DATA = "userData";

    private String mLastTwo;
    private String mCardType;
    private VisaCheckoutShippingAddress mShippingAddress;
    private VisaCheckoutUserData mUserData;

    public String getLastTwo() {
        return mLastTwo;
    }

    public String getCardType() {
        return mCardType;
    }

    public VisaCheckoutShippingAddress getShippingAddress() {
        return mShippingAddress;
    }

    public VisaCheckoutUserData getUserData() {
        return mUserData;
    }

    @Override
    public String getTypeLabel() {
        return "Visa Checkout";
    }

    public static VisaCheckoutPaymentMethodNonce fromJson(String json) throws JSONException {
        VisaCheckoutPaymentMethodNonce visaCheckoutPaymentMethodNonce = new VisaCheckoutPaymentMethodNonce();
        visaCheckoutPaymentMethodNonce.fromJson(PaymentMethodNonce.getJsonObjectForType(API_RESOURCE_KEY, json));
        return visaCheckoutPaymentMethodNonce;
    }

    @Override
    protected void fromJson(JSONObject json) throws JSONException {
        super.fromJson(json);

        JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
        mLastTwo = details.getString(LAST_TWO_KEY);
        mCardType = details.getString(CARD_TYPE_KEY);

        mShippingAddress = new VisaCheckoutShippingAddress(json.getJSONObject(SHIPPING_ADDRESS));
        mUserData = new VisaCheckoutUserData(json.getJSONObject(USER_DATA));
    }

    private VisaCheckoutPaymentMethodNonce() {}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mLastTwo);
        dest.writeString(mCardType);
        dest.writeParcelable(mShippingAddress, flags);
        dest.writeParcelable(mUserData, flags);
    }

    protected VisaCheckoutPaymentMethodNonce(Parcel in) {
        super(in);
        mLastTwo = in.readString();
        mCardType = in.readString();
        mShippingAddress = in.readParcelable(VisaCheckoutShippingAddress.class.getClassLoader());
        mUserData = in.readParcelable(VisaCheckoutUserData.class.getClassLoader());
    }

    public static final Creator<VisaCheckoutPaymentMethodNonce> CREATOR =
            new Creator<VisaCheckoutPaymentMethodNonce>() {
                @Override
                public VisaCheckoutPaymentMethodNonce createFromParcel(Parcel in) {
                    return new VisaCheckoutPaymentMethodNonce(in);
                }

                @Override
                public VisaCheckoutPaymentMethodNonce[] newArray(int size) {
                    return new VisaCheckoutPaymentMethodNonce[size];
                }
            };

}
