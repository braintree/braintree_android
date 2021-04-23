package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.braintreepayments.api.GraphQLConstants.Keys;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Use to construct a card tokenization request.
 */
public class Card extends BaseCard implements GraphQLTokenizable, Parcelable {

    private static final String GRAPHQL_CLIENT_SDK_METADATA_KEY = "clientSdkMetadata";

    private static final String MERCHANT_ACCOUNT_ID_KEY = "merchantAccountId";
    private static final String AUTHENTICATION_INSIGHT_REQUESTED_KEY = "authenticationInsight";
    private static final String AUTHENTICATION_INSIGHT_INPUT_KEY = "authenticationInsightInput";

    private String merchantAccountId;
    private boolean authenticationInsightRequested;

    @Override
    public JSONObject buildGraphQLJSON() throws BraintreeException {
        JSONObject base = new JSONObject();
        JSONObject input = new JSONObject();
        JSONObject variables = new JSONObject();

        try {
            base.put(GRAPHQL_CLIENT_SDK_METADATA_KEY, buildMetadataJSON());

            JSONObject optionsJson = new JSONObject();
            if (hasValueForValidate()) {
                optionsJson.put(VALIDATE_KEY, getValidate());
            }
            input.put(OPTIONS_KEY, optionsJson);
            variables.put(Keys.INPUT, input);

            if (TextUtils.isEmpty(mMerchantAccountId) && mAuthenticationInsightRequested) {
                throw new BraintreeException("A merchant account ID is required when authenticationInsightRequested is true.");
            }

            if (mAuthenticationInsightRequested) {
                variables.put(AUTHENTICATION_INSIGHT_INPUT_KEY, new JSONObject().put(MERCHANT_ACCOUNT_ID_KEY, mMerchantAccountId));
            }

            base.put(Keys.QUERY, getCardTokenizationGraphQLMutation());
            base.put(OPERATION_NAME_KEY, "TokenizeCreditCard");

            JSONObject creditCard = new JSONObject()
                    .put(NUMBER_KEY, mNumber)
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
            base.put(Keys.VARIABLES, variables);
        } catch (JSONException ignored) {}

        return base;
    }

    public Card() {
    }

    /**
     * @param id The merchant account id used to generate the authentication insight.
     */
    public void setMerchantAccountId(String id) {
        merchantAccountId = TextUtils.isEmpty(id) ? null : id;
    }

    /**
     * @param requested If authentication insight will be requested.
     */
    public void setAuthenticationInsightRequested(boolean requested) {
        authenticationInsightRequested = requested;
    }

    @Override
    JSONObject buildJSON() {
        JSONObject json = super.buildJSON();
        if (mAuthenticationInsightRequested) {
            try {
                json.put(MERCHANT_ACCOUNT_ID_KEY, mMerchantAccountId);
                json.put(AUTHENTICATION_INSIGHT_REQUESTED_KEY, mAuthenticationInsightRequested);
            } catch (JSONException exception) {
                exception.printStackTrace();
            }
        }
        return json;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(merchantAccountId);
        dest.writeByte(authenticationInsightRequested ? (byte) 1 : 0);
    }

    protected Card(Parcel in) {
        super(in);
        merchantAccountId = in.readString();
        authenticationInsightRequested = in.readByte() > 0;
    }

    public static final Creator<Card> CREATOR = new Creator<Card>() {
        @Override
        public Card createFromParcel(Parcel in) {
            return new Card(in);
        }

        @Override
        public Card[] newArray(int size) {
            return new Card[size];
        }
    };

    private String getCardTokenizationGraphQLMutation() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mutation TokenizeCreditCard($input: TokenizeCreditCardInput!");

        if (authenticationInsightRequested) {
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

        if (authenticationInsightRequested) {
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