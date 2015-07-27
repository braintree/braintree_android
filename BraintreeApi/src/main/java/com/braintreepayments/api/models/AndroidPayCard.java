package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.annotations.Beta;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link com.braintreepayments.api.models.PaymentMethod} representing an Android Pay card.
 * @see com.braintreepayments.api.models.PaymentMethod
 */
@Beta
public class AndroidPayCard extends PaymentMethod implements Parcelable {

    protected static final String PAYMENT_METHOD_TYPE = "AndroidPayCard";
    protected static final String API_RESOURCE_KEY = "androidPayCards";

    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";
    private static final String LAST_TWO_KEY = "lastTwo";

    private String mCardType;
    private String mLastTwo;

    /**
     * Convert an API response to an {@link AndroidPayCard}.
     *
     * @param json Raw JSON response from Braintree of a {@link AndroidPayCard}.
     * @return {@link AndroidPayCard}.
     * @throws JSONException when parsing the response fails.
     */
    public static AndroidPayCard fromJson(String json) throws JSONException {
        AndroidPayCard androidPayCard = new AndroidPayCard();
        androidPayCard.fromJson(AndroidPayCard.getJsonObjectForType(API_RESOURCE_KEY, json));
        return androidPayCard;
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

    public AndroidPayCard() {}

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

    private AndroidPayCard(Parcel in) {
        mNonce = in.readString();
        mDescription = in.readString();
        mCardType = in.readString();
        mLastTwo = in.readString();
    }

    public static final Creator<AndroidPayCard> CREATOR = new Creator<AndroidPayCard>() {
        public AndroidPayCard createFromParcel(Parcel source) {
            return new AndroidPayCard(source);
        }

        public AndroidPayCard[] newArray(int size) {
            return new AndroidPayCard[size];
        }
    };
}
