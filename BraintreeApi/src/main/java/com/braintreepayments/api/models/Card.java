package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.annotations.Beta;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link com.braintreepayments.api.models.PaymentMethod} representing a credit or debit card.
 */
public class Card extends PaymentMethod implements Parcelable {

    protected static final String PAYMENT_METHOD_TYPE = "CreditCard";
    protected static final String API_RESOURCE_KEY = "creditCards";

    private static final String THREE_D_SECURE_INFO_KEY = "threeDSecureInfo";
    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";
    private static final String LAST_TWO_KEY = "lastTwo";

    private ThreeDSecureInfo mThreeDSecureInfo;
    private String mCardType;
    private String mLastTwo;

    /**
     * Convert an API response to a {@link Card}.
     *
     * @param json Raw JSON response from Braintree of a {@link Card}.
     * @return {@link Card}.
     * @throws JSONException when parsing the response fails.
     */
    public static Card fromJson(String json) throws JSONException {
        Card card = new Card();
        card.fromJson(Card.getJsonObjectForType(API_RESOURCE_KEY, json));
        return card;
    }

    /**
     * Populate properties with values from a {@link JSONObject} .
     *
     * @param json {@link JSONObject}
     * @throws JSONException when parsing fails.
     */
    protected void fromJson(JSONObject json) throws JSONException {
        super.fromJson(json);

        mThreeDSecureInfo =
                ThreeDSecureInfo.fromJson(json.optJSONObject(THREE_D_SECURE_INFO_KEY));

        JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
        mLastTwo = details.getString(LAST_TWO_KEY);
        mCardType = details.getString(CARD_TYPE_KEY);
    }

    /**
     * @return Type of this card (e.g. MasterCard, American Express)
     */
    @Override
    public String getTypeLabel() {
        return mCardType;
    }

    /**
     * @return Last two digits of the card, intended for display purposes.
     */
    public String getLastTwo() {
        return mLastTwo;
    }

    /**
     * @return The 3D Secure info for the current {@link com.braintreepayments.api.models.Card} or
     * {@code null}
     */
    @Beta
    public ThreeDSecureInfo getThreeDSecureInfo() {
        return mThreeDSecureInfo;
    }

    public Card() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mThreeDSecureInfo, flags);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeString(mCardType);
        dest.writeString(mLastTwo);
    }

    protected Card(Parcel in) {
        mThreeDSecureInfo = in.readParcelable(ThreeDSecureInfo.class.getClassLoader());
        mNonce = in.readString();
        mDescription = in.readString();
        mCardType = in.readString();
        mLastTwo = in.readString();
    }

    public static final Creator<Card> CREATOR = new Creator<Card>() {
        public Card createFromParcel(Parcel source) {
            return new Card(source);
        }

        public Card[] newArray(int size) {
            return new Card[size];
        }
    };
}