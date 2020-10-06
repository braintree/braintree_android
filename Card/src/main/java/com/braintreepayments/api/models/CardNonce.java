package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.braintreepayments.api.Json;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.models.BinData.BIN_DATA_KEY;

/**
 * {@link PaymentMethodNonce} representing a credit or debit card.
 */
public class CardNonce extends PaymentMethodNonce implements Parcelable {

    protected static final String TYPE = "CreditCard";
    protected static final String API_RESOURCE_KEY = "creditCards";

    private static final String GRAPHQL_TOKENIZE_CREDIT_CARD_KEY = "tokenizeCreditCard";
    private static final String GRAPHQL_CREDIT_CARD_KEY = "creditCard";
    private static final String GRAPHQL_BRAND_KEY = "brand";
    private static final String GRAPHQL_LAST_FOUR_KEY = "last4";
    private static final String THREE_D_SECURE_INFO_KEY = "threeDSecureInfo";
    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";
    private static final String LAST_TWO_KEY = "lastTwo";
    private static final String LAST_FOUR_KEY = "lastFour";
    private static final String BIN_KEY = "bin";
    private static final String AUTHENTICATION_INSIGHT_KEY = "authenticationInsight";
    private static final String EXPIRATION_MONTH_KEY = "expirationMonth";
    private static final String EXPIRATION_YEAR_KEY = "expirationYear";
    private static final String CARDHOLDER_NAME_KEY = "cardholderName";

    private String mCardType;
    private String mLastTwo;
    private String mLastFour;
    private ThreeDSecureInfo mThreeDSecureInfo;
    private String mBin;
    private BinData mBinData;
    private AuthenticationInsight mAuthenticationInsight;
    private String mExpirationMonth;
    private String mExpirationYear;
    private String mCardholderName;

    /**
     * Convert an API response to a {@link CardNonce}.
     *
     * @param json Raw JSON response from Braintree of a {@link CardNonce}.
     * @return {@link CardNonce}.
     * @throws JSONException when parsing the response fails.
     */
    public static CardNonce fromJson(String json) throws JSONException {
        CardNonce cardNonce = new CardNonce();
        JSONObject jsonObject = new JSONObject(json);

        if (jsonObject.has(DATA_KEY)) {
            cardNonce.fromGraphQLJson(jsonObject);
        } else {
            cardNonce.fromJson(CardNonce.getJsonObjectForType(API_RESOURCE_KEY, jsonObject));
        }

        return cardNonce;
    }

    /**
     * Populate properties with values from a {@link JSONObject} .
     *
     * @param json {@link JSONObject}
     * @throws JSONException when parsing fails.
     */
    protected void fromJson(JSONObject json) throws JSONException {
        super.fromJson(json);

        JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
        mLastTwo = details.getString(LAST_TWO_KEY);
        mLastFour = details.getString(LAST_FOUR_KEY);
        mCardType = details.getString(CARD_TYPE_KEY);
        mThreeDSecureInfo = ThreeDSecureInfo.fromJson(json.optJSONObject(THREE_D_SECURE_INFO_KEY));
        mBin = Json.optString(details, BIN_KEY, "");
        mBinData = BinData.fromJson(json.optJSONObject(BIN_DATA_KEY));
        mAuthenticationInsight = AuthenticationInsight.fromJson(json.optJSONObject(AUTHENTICATION_INSIGHT_KEY));
        mExpirationMonth = Json.optString(details, EXPIRATION_MONTH_KEY, "");
        mExpirationYear = Json.optString(details, EXPIRATION_YEAR_KEY, "");
        mCardholderName = Json.optString(details, CARDHOLDER_NAME_KEY, "");
    }

    private void fromGraphQLJson(JSONObject json) throws JSONException {
        JSONObject data = json.getJSONObject(DATA_KEY);

        if (data.has(GRAPHQL_TOKENIZE_CREDIT_CARD_KEY)) {
            JSONObject payload = data.getJSONObject(GRAPHQL_TOKENIZE_CREDIT_CARD_KEY);

            JSONObject creditCard = payload.getJSONObject(GRAPHQL_CREDIT_CARD_KEY);
            mLastFour = Json.optString(creditCard, GRAPHQL_LAST_FOUR_KEY, "");
            mLastTwo = mLastFour.length() < 4 ? "" : mLastFour.substring(2);
            mCardType = Json.optString(creditCard, GRAPHQL_BRAND_KEY, "Unknown");
            mThreeDSecureInfo = ThreeDSecureInfo.fromJson(null);
            mBin = Json.optString(creditCard, "bin", "");
            mBinData = BinData.fromJson(creditCard.optJSONObject(BIN_DATA_KEY));
            mNonce = payload.getString(TOKEN_KEY);
            mDescription = TextUtils.isEmpty(mLastTwo) ? "" : "ending in ••" + mLastTwo;
            mDefault = false;
            mAuthenticationInsight = AuthenticationInsight.fromJson(payload.optJSONObject(AUTHENTICATION_INSIGHT_KEY));
            mExpirationMonth = Json.optString(creditCard, EXPIRATION_MONTH_KEY, "");
            mExpirationYear = Json.optString(creditCard, EXPIRATION_YEAR_KEY, "");
            mCardholderName = Json.optString(creditCard, CARDHOLDER_NAME_KEY, "");
        } else {
            throw new JSONException("Failed to parse GraphQL response JSON");
        }
    }

    /**
     * @return Type of this card (e.g. MasterCard, American Express)
     */
    @Override
    public String getTypeLabel() {
        return mCardType;
    }

    /**
     * @return Type of this card (e.g. Visa, MasterCard, American Express)
     */
    public String getCardType() {
        return mCardType;
    }

    /**
     * @return Last two digits of the card, intended for display purposes.
     */
    public String getLastTwo() {
        return mLastTwo;
    }

    /**
     * @return Last four digits of the card.
     */
    public String getLastFour() {
        return mLastFour;
    }

    /**
     * @return The expiration month of the card.
     */
    public String getExpirationMonth() {
        return mExpirationMonth;
    }

    /**
     * @return The expiration year of the card.
     */
    public String getExpirationYear() {
        return mExpirationYear;
    }

    /**
     * @return The name of the cardholder.
     */
    public String getCardholderName() {
        return mCardholderName;
    }

    /**
     * @return The 3D Secure info for the current {@link CardNonce} or
     * {@code null}
     */
    public ThreeDSecureInfo getThreeDSecureInfo() {
        return mThreeDSecureInfo;
    }

    /**
     * @return BIN of the card.
     */
    public String getBin() {
        return mBin;
    }

    /**
     * @return The BIN data for the card number associated with {@link CardNonce}
     */
    public BinData getBinData() {
        return mBinData;
    }

    /**
     * @return {@link AuthenticationInsight}
     * Details about the regulatory environment and applicable customer authentication regulation
     * for a potential transaction. You may use this to make an informed decision whether to perform
     * 3D Secure authentication.
     */
    public AuthenticationInsight getAuthenticationInsight() {
        return mAuthenticationInsight;
    }

    public CardNonce() {}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mCardType);
        dest.writeString(mLastTwo);
        dest.writeString(mLastFour);
        dest.writeParcelable(mBinData, flags);
        dest.writeParcelable(mThreeDSecureInfo, flags);
        dest.writeParcelable(mAuthenticationInsight, flags);
        dest.writeString(mExpirationMonth);
        dest.writeString(mExpirationYear);
        dest.writeString(mCardholderName);
    }

    protected CardNonce(Parcel in) {
        super(in);
        mCardType = in.readString();
        mLastTwo = in.readString();
        mLastFour = in.readString();
        mBinData = in.readParcelable(BinData.class.getClassLoader());
        mThreeDSecureInfo = in.readParcelable(ThreeDSecureInfo.class.getClassLoader());
        mAuthenticationInsight = in.readParcelable(AuthenticationInsight.class.getClassLoader());
        mExpirationMonth = in.readString();
        mExpirationYear = in.readString();
        mCardholderName = in.readString();
    }

    public static final Creator<CardNonce> CREATOR = new Creator<CardNonce>() {
        public CardNonce createFromParcel(Parcel source) {
            return new CardNonce(source);
        }

        public CardNonce[] newArray(int size) {
            return new CardNonce[size];
        }
    };
}