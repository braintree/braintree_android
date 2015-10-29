package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link PaymentMethodNonce} representing an Android Pay card.
 * @see PaymentMethodNonce
 */
public class AndroidPayCardNonce extends PaymentMethodNonce implements Parcelable {

    protected static final String TYPE = "AndroidPayCard";
    protected static final String API_RESOURCE_KEY = "androidPayCards";

    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";
    private static final String LAST_TWO_KEY = "lastTwo";

    private String mCardType;
    private String mLastTwo;

    /**
     * Convert an API response to an {@link AndroidPayCardNonce}.
     *
     * @param json Raw JSON response from Braintree of a {@link AndroidPayCardNonce}.
     * @return {@link AndroidPayCardNonce}.
     * @throws JSONException when parsing the response fails.
     */
    public static AndroidPayCardNonce fromJson(String json) throws JSONException {
        AndroidPayCardNonce androidPayCardNonce = new AndroidPayCardNonce();
        androidPayCardNonce
                .fromJson(AndroidPayCardNonce.getJsonObjectForType(API_RESOURCE_KEY, json));
        return androidPayCardNonce;
    }

    protected void fromJson(JSONObject json) throws JSONException {
        super.fromJson(json);

        JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
        mLastTwo = details.getString(LAST_TWO_KEY);
        mCardType = details.getString(CARD_TYPE_KEY);
    }

    @Override
    public String getTypeLabel() {
        return "Google Wallet";
    }

    public String getLastTwo() {
        return mLastTwo;
    }

    public AndroidPayCardNonce() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeString(mCardType);
        dest.writeString(mLastTwo);
    }

    private AndroidPayCardNonce(Parcel in) {
        mNonce = in.readString();
        mDescription = in.readString();
        mCardType = in.readString();
        mLastTwo = in.readString();
    }

    public static final Creator<AndroidPayCardNonce> CREATOR = new Creator<AndroidPayCardNonce>() {
        public AndroidPayCardNonce createFromParcel(Parcel source) {
            return new AndroidPayCardNonce(source);
        }

        public AndroidPayCardNonce[] newArray(int size) {
            return new AndroidPayCardNonce[size];
        }
    };
}
