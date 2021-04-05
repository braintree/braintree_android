package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.BinData.BIN_DATA_KEY;

/**
 * {@link UntypedPaymentMethodNonce} representing a credit or debit card.
 */
public class CardNonce implements PaymentMethodNonce, Parcelable {

    static final String TYPE = "CreditCard";
    static final String API_RESOURCE_KEY = "creditCards";

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";
    private static final String DESCRIPTION_KEY = "description";

    static final String DATA_KEY = "data";
    static final String TOKEN_KEY = "token";

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

    protected String mNonce;
    protected String mDescription;
    protected boolean mDefault;

    CardNonce(String jsonString) throws JSONException {
        this(new JSONObject(jsonString));
    }

    CardNonce(JSONObject inputJson) throws JSONException {
        if (inputJson.has(DATA_KEY)) {
            JSONObject data = inputJson.getJSONObject(DATA_KEY);

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
        } else {
            JSONObject json;
            if (inputJson.has(API_RESOURCE_KEY)) {
                json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
            } else {
                json = inputJson;
            }

            mNonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
            mDescription = json.getString(DESCRIPTION_KEY);
            mDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

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
    }

    /**
     * @return Type of this card (e.g. MasterCard, American Express)
     */
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mCardType);
        dest.writeString(mLastTwo);
        dest.writeString(mLastFour);
        dest.writeParcelable(mBinData, flags);
        dest.writeParcelable(mThreeDSecureInfo, flags);
        dest.writeParcelable(mAuthenticationInsight, flags);
        dest.writeString(mExpirationMonth);
        dest.writeString(mExpirationYear);
        dest.writeString(mCardholderName);
        dest.writeString(mNonce);
        dest.writeString(mDescription);
        dest.writeByte(mDefault ? (byte) 1 : (byte) 0);
    }

    protected CardNonce(Parcel in) {
        mCardType = in.readString();
        mLastTwo = in.readString();
        mLastFour = in.readString();
        mBinData = in.readParcelable(BinData.class.getClassLoader());
        mThreeDSecureInfo = in.readParcelable(ThreeDSecureInfo.class.getClassLoader());
        mAuthenticationInsight = in.readParcelable(AuthenticationInsight.class.getClassLoader());
        mExpirationMonth = in.readString();
        mExpirationYear = in.readString();
        mCardholderName = in.readString();
        mNonce = in.readString();
        mDescription = in.readString();
        mDefault = in.readByte() > 0;
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