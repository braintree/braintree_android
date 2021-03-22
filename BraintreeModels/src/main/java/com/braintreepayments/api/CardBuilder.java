package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.braintreepayments.api.GraphQLConstants.Keys;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builder used to construct a card tokenization request.
 */
public class CardBuilder extends BaseCardBuilder implements Parcelable {

    private static final String MERCHANT_ACCOUNT_ID_KEY = "merchantAccountId";
    private static final String AUTHENTICATION_INSIGHT_REQUESTED_KEY = "authenticationInsight";
    private static final String AUTHENTICATION_INSIGHT_INPUT_KEY = "authenticationInsightInput";

    private String mMerchantAccountId;
    private boolean mAuthenticationInsightRequested;

    protected void buildGraphQL(JSONObject base, JSONObject variables) throws BraintreeException, JSONException {
        JSONObject input = variables.getJSONObject(Keys.INPUT);

        if (TextUtils.isEmpty(mMerchantAccountId) && mAuthenticationInsightRequested) {
            throw new BraintreeException("A merchant account ID is required when authenticationInsightRequested is true.");
        }

        if (mAuthenticationInsightRequested) {
            variables.put(AUTHENTICATION_INSIGHT_INPUT_KEY, new JSONObject().put(MERCHANT_ACCOUNT_ID_KEY, mMerchantAccountId));
        }

        base.put(Keys.QUERY, getCardTokenizationGraphQLMutation());
        base.put(OPERATION_NAME_KEY, "TokenizeCreditCard");

        JSONObject creditCard = new JSONObject()
                .put(NUMBER_KEY, mCardnumber)
                .put(EXPIRATION_MONTH_KEY, mExpirationMonth)
                .put(EXPIRATION_YEAR_KEY, mExpirationYear)
                .put(CVV_KEY, mCvv)
                .put(CARDHOLDER_NAME_KEY, mCardholderName);

        JSONObject billingAddress = new JSONObject()
                .put(FIRST_NAME_KEY, mFirstName)
                .put(LAST_NAME_KEY, mLastName)
                .put(COMPANY_KEY, mCompany)
                .put(COUNTRY_CODE_KEY, mCountryCode)
                .put(LOCALITY_KEY, mLocality)
                .put(POSTAL_CODE_KEY, mPostalCode)
                .put(REGION_KEY, mRegion)
                .put(STREET_ADDRESS_KEY, mStreetAddress)
                .put(EXTENDED_ADDRESS_KEY, mExtendedAddress);

        if (billingAddress.length() > 0) {
            creditCard.put(BILLING_ADDRESS_KEY, billingAddress);
        }

        input.put(CREDIT_CARD_KEY, creditCard);
    }

    public CardBuilder() {
    }

    /**
     * @param id The merchant account id used to generate the authentication insight.
     */
    public void merchantAccountId(String id) {
        mMerchantAccountId = TextUtils.isEmpty(id) ? null : id;
    }

    /**
     * @param requested If authentication insight will be requested.
     */
    public void authenticationInsightRequested(boolean requested) {
        mAuthenticationInsightRequested = requested;
    }

    @Override
    protected void build(JSONObject json, JSONObject paymentMethodNonceJson) throws JSONException {
        super.build(json, paymentMethodNonceJson);

        if (mAuthenticationInsightRequested) {
            json.put(MERCHANT_ACCOUNT_ID_KEY, mMerchantAccountId);
            json.put(AUTHENTICATION_INSIGHT_REQUESTED_KEY, mAuthenticationInsightRequested);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mMerchantAccountId);
        dest.writeByte(mAuthenticationInsightRequested ? (byte) 1 : 0);
    }

    protected CardBuilder(Parcel in) {
        super(in);
        mMerchantAccountId = in.readString();
        mAuthenticationInsightRequested = in.readByte() > 0;
    }

    public static final Creator<CardBuilder> CREATOR = new Creator<CardBuilder>() {
        @Override
        public CardBuilder createFromParcel(Parcel in) {
            return new CardBuilder(in);
        }

        @Override
        public CardBuilder[] newArray(int size) {
            return new CardBuilder[size];
        }
    };

    private String getCardTokenizationGraphQLMutation() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mutation TokenizeCreditCard($input: TokenizeCreditCardInput!");

        if (mAuthenticationInsightRequested) {
            stringBuilder.append(", $authenticationInsightInput: AuthenticationInsightInput!");
        }

        stringBuilder.append(") {" +
                "  tokenizeCreditCard(input: $input) {" +
                "    token" +
                "    creditCard {" +
                "      bin" +
                "      brand" +
                "      expirationMonth" +
                "      expirationYear" +
                "      cardholderName" +
                "      last4" +
                "      binData {" +
                "        prepaid" +
                "        healthcare" +
                "        debit" +
                "        durbinRegulated" +
                "        commercial" +
                "        payroll" +
                "        issuingBank" +
                "        countryOfIssuance" +
                "        productId" +
                "      }" +
                "    }");

        if (mAuthenticationInsightRequested) {
            stringBuilder.append("" +
                    "    authenticationInsight(input: $authenticationInsightInput) {" +
                    "      customerAuthenticationRegulationEnvironment" +
                    "    }");
        }

        stringBuilder.append("" +
                "  }" +
                "}");

        return stringBuilder.toString();
    }
}