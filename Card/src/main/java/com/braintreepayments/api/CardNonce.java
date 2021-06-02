package com.braintreepayments.api;

import android.os.Parcel;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.BinData.BIN_DATA_KEY;

/**
 * {@link PaymentMethodNonce} representing a credit or debit card.
 */
public class CardNonce extends PaymentMethodNonce {

    private static final String API_RESOURCE_KEY = "creditCards";

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";

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

    private final String cardType;
    private final String lastTwo;
    private final String lastFour;
    private final ThreeDSecureInfo threeDSecureInfo;
    private final String bin;
    private final BinData binData;
    private final AuthenticationInsight authenticationInsight;
    private final String expirationMonth;
    private final String expirationYear;
    private final String cardholderName;

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

        return new CardNonce(cardType, lastTwo, lastFour, threeDSecureInfo, bin, binData, authenticationInsight, expirationMonth, expirationYear, cardholderName, nonce, isDefault);
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
            AuthenticationInsight authenticationInsight = AuthenticationInsight.fromJson(payload.optJSONObject(AUTHENTICATION_INSIGHT_KEY));
            String expirationMonth = Json.optString(creditCard, EXPIRATION_MONTH_KEY, "");
            String expirationYear = Json.optString(creditCard, EXPIRATION_YEAR_KEY, "");
            String cardholderName = Json.optString(creditCard, CARDHOLDER_NAME_KEY, "");

            return new CardNonce(cardType, lastTwo, lastFour, threeDSecureInfo, bin, binData, authenticationInsight, expirationMonth, expirationYear, cardholderName, nonce, false);

        } else {
            throw new JSONException("Failed to parse GraphQL response JSON");
        }
    }

    private CardNonce(String cardType, String lastTwo, String lastFour, ThreeDSecureInfo threeDSecureInfo, String bin, BinData binData, AuthenticationInsight authenticationInsight, String expirationMonth, String expirationYear, String cardholderName, String nonce, boolean isDefault) {
        super(nonce, isDefault);
        this.cardType = cardType;
        this.lastTwo = lastTwo;
        this.lastFour = lastFour;
        this.threeDSecureInfo = threeDSecureInfo;
        this.bin = bin;
        this.binData = binData;
        this.authenticationInsight = authenticationInsight;
        this.expirationMonth = expirationMonth;
        this.expirationYear = expirationYear;
        this.cardholderName = cardholderName;
    }

    /**
     * @return Type of this card (e.g. Visa, MasterCard, American Express)
     */
    @Nullable
    public String getCardType() {
        return cardType;
    }

    /**
     * @return Last two digits of the card, intended for display purposes.
     */
    @Nullable
    public String getLastTwo() {
        return lastTwo;
    }

    /**
     * @return Last four digits of the card.
     */
    @Nullable
    public String getLastFour() {
        return lastFour;
    }

    /**
     * @return The expiration month of the card.
     */
    @Nullable
    public String getExpirationMonth() {
        return expirationMonth;
    }

    /**
     * @return The expiration year of the card.
     */
    @Nullable
    public String getExpirationYear() {
        return expirationYear;
    }

    /**
     * @return The name of the cardholder.
     */
    @Nullable
    public String getCardholderName() {
        return cardholderName;
    }

    /**
     * @return The 3D Secure info for the current {@link CardNonce} or
     * {@code null}
     */
    @Nullable
    public ThreeDSecureInfo getThreeDSecureInfo() {
        return threeDSecureInfo;
    }

    /**
     * @return BIN of the card.
     */
    @Nullable
    public String getBin() {
        return bin;
    }

    /**
     * @return The BIN data for the card number associated with {@link CardNonce}
     */
    @Nullable
    public BinData getBinData() {
        return binData;
    }

    /**
     * @return {@link AuthenticationInsight}
     * Details about the regulatory environment and applicable customer authentication regulation
     * for a potential transaction. You may use this to make an informed decision whether to perform
     * 3D Secure authentication.
     */
    @Nullable
    public AuthenticationInsight getAuthenticationInsight() {
        return authenticationInsight;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(cardType);
        dest.writeString(lastTwo);
        dest.writeString(lastFour);
        dest.writeString(bin);
        dest.writeParcelable(binData, flags);
        dest.writeParcelable(threeDSecureInfo, flags);
        dest.writeParcelable(authenticationInsight, flags);
        dest.writeString(expirationMonth);
        dest.writeString(expirationYear);
        dest.writeString(cardholderName);
    }

    protected CardNonce(Parcel in) {
        super(in);
        cardType = in.readString();
        lastTwo = in.readString();
        lastFour = in.readString();
        bin = in.readString();
        binData = in.readParcelable(BinData.class.getClassLoader());
        threeDSecureInfo = in.readParcelable(ThreeDSecureInfo.class.getClassLoader());
        authenticationInsight = in.readParcelable(AuthenticationInsight.class.getClassLoader());
        expirationMonth = in.readString();
        expirationYear = in.readString();
        cardholderName = in.readString();
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