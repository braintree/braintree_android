package com.braintreepayments.api;

import android.os.Parcel;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.BinData.BIN_DATA_KEY;

/**
 * {@link PaymentMethodNonce} representing a credit or debit card.
 */
public class CardNonce extends PaymentMethodNonce {

    static final String TYPE = "CreditCard";
    private static final String API_RESOURCE_KEY = "creditCards";

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";
    private static final String DESCRIPTION_KEY = "description";

    private static final String DATA_KEY = "data";
    private static final String TOKEN_KEY = "token";

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

    private final String mCardType;
    private final String mLastTwo;
    private final String mLastFour;
    private final ThreeDSecureInfo mThreeDSecureInfo;
    private final String mBin;
    private final BinData mBinData;
    private final AuthenticationInsight mAuthenticationInsight;
    private final String mExpirationMonth;
    private final String mExpirationYear;
    private final String mCardholderName;

    private final String mNonce;
    private final String mDescription;
    private final boolean mDefault;

    /**
     * Parse card nonce from plain JSON object.
     * @param inputJson plain JSON object
     * @return {@link CardNonce}
     * @throws JSONException if nonce could not be parsed successfully
     */
    @NonNull
    static CardNonce fromJSON(JSONObject inputJson) throws JSONException {
        if (isGraphQLTokenizationResponse(inputJson)) {
            return CardNonce.fromGraphQLJSON(inputJson);
        } else if (isRESTfulTokenizationResponse(inputJson)) {
            return CardNonce.fromRESTJSON(inputJson);
        } else {
            return CardNonce.fromPlainJSONObject(inputJson);
        }
    }

    private static boolean isGraphQLTokenizationResponse(JSONObject inputJSON) {
        return inputJSON.has(DATA_KEY);
    }

    private static boolean isRESTfulTokenizationResponse(JSONObject inputJSON) {
        return inputJSON.has(API_RESOURCE_KEY);
    }

    /**
     * Parse card nonce from RESTful Tokenization response.
     * @param inputJson plain JSON object
     * @return {@link CardNonce}
     * @throws JSONException if nonce could not be parsed successfully
     */
    @NonNull
    private static CardNonce fromRESTJSON(JSONObject inputJson) throws JSONException {
        JSONObject json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        return CardNonce.fromPlainJSONObject(json);
    }

    /**
     * Parse card nonce from RESTful Tokenization response.
     * @param inputJson plain JSON object
     * @return {@link CardNonce}
     * @throws JSONException if nonce could not be parsed successfully
     */
    @NonNull
    private static CardNonce fromPlainJSONObject(JSONObject inputJson) throws JSONException {
        String nonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY);
        String description = inputJson.getString(DESCRIPTION_KEY);
        boolean isDefault = inputJson.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

        JSONObject details = inputJson.getJSONObject(CARD_DETAILS_KEY);
        String lastTwo = details.getString(LAST_TWO_KEY);
        String lastFour = details.getString(LAST_FOUR_KEY);
        String cardType = details.getString(CARD_TYPE_KEY);
        ThreeDSecureInfo threeDSecureInfo = ThreeDSecureInfo.fromJson(inputJson.optJSONObject(THREE_D_SECURE_INFO_KEY));
        String bin = Json.optString(details, BIN_KEY, "");
        BinData binData = BinData.fromJson(inputJson.optJSONObject(BIN_DATA_KEY));
        AuthenticationInsight authenticationInsight = AuthenticationInsight.fromJson(inputJson.optJSONObject(AUTHENTICATION_INSIGHT_KEY));
        String expirationMonth = Json.optString(details, EXPIRATION_MONTH_KEY, "");
        String expirationYear = Json.optString(details, EXPIRATION_YEAR_KEY, "");
        String cardholderName = Json.optString(details, CARDHOLDER_NAME_KEY, "");

        return new CardNonce(cardType, lastTwo, lastFour, threeDSecureInfo, bin, binData, authenticationInsight, expirationMonth, expirationYear, cardholderName, nonce, description, isDefault);
    }

    /**
     * Parse card nonce from GraphQL Tokenization response.
     * @param inputJson plain JSON object
     * @return {@link CardNonce}
     * @throws JSONException if nonce could not be parsed successfully
     */
    @NonNull
    private static CardNonce fromGraphQLJSON(JSONObject inputJson) throws JSONException {
        JSONObject data = inputJson.getJSONObject(DATA_KEY);

        if (data.has(GRAPHQL_TOKENIZE_CREDIT_CARD_KEY)) {
            JSONObject payload = data.getJSONObject(GRAPHQL_TOKENIZE_CREDIT_CARD_KEY);

            JSONObject creditCard = payload.getJSONObject(GRAPHQL_CREDIT_CARD_KEY);
            String lastFour = Json.optString(creditCard, GRAPHQL_LAST_FOUR_KEY, "");
            String lastTwo = lastFour.length() < 4 ? "" : lastFour.substring(2);
            String cardType = Json.optString(creditCard, GRAPHQL_BRAND_KEY, "Unknown");
            ThreeDSecureInfo threeDSecureInfo = ThreeDSecureInfo.fromJson(null);
            String bin = Json.optString(creditCard, "bin", "");
            BinData binData = BinData.fromJson(creditCard.optJSONObject(BIN_DATA_KEY));
            String nonce = payload.getString(TOKEN_KEY);
            String description = TextUtils.isEmpty(lastTwo) ? "" : "ending in ••" + lastTwo;
            boolean isDefault = false;
            AuthenticationInsight authenticationInsight = AuthenticationInsight.fromJson(payload.optJSONObject(AUTHENTICATION_INSIGHT_KEY));
            String expirationMonth = Json.optString(creditCard, EXPIRATION_MONTH_KEY, "");
            String expirationYear = Json.optString(creditCard, EXPIRATION_YEAR_KEY, "");
            String cardholderName = Json.optString(creditCard, CARDHOLDER_NAME_KEY, "");

            return new CardNonce(cardType, lastTwo, lastFour, threeDSecureInfo, bin, binData, authenticationInsight, expirationMonth, expirationYear, cardholderName, nonce, description, isDefault);

        } else {
            throw new JSONException("Failed to parse GraphQL response JSON");
        }
    }

    private CardNonce(String cardType, String lastTwo, String lastFour, ThreeDSecureInfo threeDSecureInfo, String bin, BinData binData, AuthenticationInsight authenticationInsight, String expirationMonth, String expirationYear, String cardholderName, String nonce, String description, boolean isDefault) {
        super(nonce, description, isDefault, "TODO", PaymentMethodType.CARD);
        mCardType = cardType;
        mLastTwo = lastTwo;
        mLastFour = lastFour;
        mThreeDSecureInfo = threeDSecureInfo;
        mBin = bin;
        mBinData = binData;
        mAuthenticationInsight = authenticationInsight;
        mExpirationMonth = expirationMonth;
        mExpirationYear = expirationYear;
        mCardholderName = cardholderName;
        mNonce = nonce;
        mDescription = description;
        mDefault = isDefault;
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

    /**
     * @inheritDoc
     */
    public String getString() {
        return mNonce;
    }

    /**
     * @inheritDoc
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * @inheritDoc
     */
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
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mCardType);
        dest.writeString(mLastTwo);
        dest.writeString(mLastFour);
        dest.writeString(mBin);
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
        super(in);
        mCardType = in.readString();
        mLastTwo = in.readString();
        mLastFour = in.readString();
        mBin = in.readString();
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