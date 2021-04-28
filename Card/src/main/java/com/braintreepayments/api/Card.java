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
public class Card extends BaseCard implements Parcelable {

    private static final String GRAPHQL_CLIENT_SDK_METADATA_KEY = "clientSdkMetadata";

    private static final String MERCHANT_ACCOUNT_ID_KEY = "merchantAccountId";
    private static final String AUTHENTICATION_INSIGHT_REQUESTED_KEY = "authenticationInsight";
    private static final String AUTHENTICATION_INSIGHT_INPUT_KEY = "authenticationInsightInput";

    private String merchantAccountId;
    private boolean authenticationInsightRequested;

    private boolean shouldValidate;

    JSONObject buildJSONForGraphQL() throws BraintreeException, JSONException {
        JSONObject base = new JSONObject();
        JSONObject input = new JSONObject();
        JSONObject variables = new JSONObject();

        base.put(GRAPHQL_CLIENT_SDK_METADATA_KEY, buildMetadataJSON());

        JSONObject optionsJson = new JSONObject();
        optionsJson.put(VALIDATE_KEY, shouldValidate);
        input.put(OPTIONS_KEY, optionsJson);
        variables.put(Keys.INPUT, input);

        if (TextUtils.isEmpty(merchantAccountId) && authenticationInsightRequested) {
            throw new BraintreeException("A merchant account ID is required when authenticationInsightRequested is true.");
        }

        if (authenticationInsightRequested) {
            variables.put(AUTHENTICATION_INSIGHT_INPUT_KEY, new JSONObject().put(MERCHANT_ACCOUNT_ID_KEY, merchantAccountId));
        }

        base.put(Keys.QUERY, getCardTokenizationGraphQLMutation());
        base.put(OPERATION_NAME_KEY, "TokenizeCreditCard");

        JSONObject creditCard = new JSONObject()
                .put(NUMBER_KEY, number)
                .put(EXPIRATION_MONTH_KEY, expirationMonth)
                .put(EXPIRATION_YEAR_KEY, expirationYear)
                .put(CVV_KEY, cvv)
                .put(CARDHOLDER_NAME_KEY, cardholderName);

        JSONObject billingAddress = new JSONObject()
                .put(FIRST_NAME_KEY, firstName)
                .put(LAST_NAME_KEY, lastName)
                .put(COMPANY_KEY, company)
                .put(COUNTRY_CODE_KEY, countryCode)
                .put(LOCALITY_KEY, locality)
                .put(POSTAL_CODE_KEY, postalCode)
                .put(REGION_KEY, region)
                .put(STREET_ADDRESS_KEY, streetAddress)
                .put(EXTENDED_ADDRESS_KEY, extendedAddress);

        if (billingAddress.length() > 0) {
            creditCard.put(BILLING_ADDRESS_KEY, billingAddress);
        }

        input.put(CREDIT_CARD_KEY, creditCard);
        base.put(Keys.VARIABLES, variables);

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
     * @param shouldValidate Flag to denote when the associated {@link PaymentMethodNonce}
     *                       will be validated. When set to {@code true}, the {@link PaymentMethodNonce}
     *                       will be validated immediately. When {@code false}, the {@link PaymentMethodNonce}
     *                       will be validated when used by a server side library for a Braintree gateway action.
     */
    public void setShouldValidate(boolean shouldValidate) {
        this.shouldValidate = shouldValidate;
    }

    /**
     * @param requested If authentication insight will be requested.
     */
    public void setAuthenticationInsightRequested(boolean requested) {
        authenticationInsightRequested = requested;
    }

    @Override
    JSONObject buildJSON() throws JSONException {
        JSONObject json = super.buildJSON();

        JSONObject paymentMethodNonceJson = json.getJSONObject(CREDIT_CARD_KEY);
        JSONObject optionsJson = new JSONObject();
        optionsJson.put(VALIDATE_KEY, shouldValidate);
        paymentMethodNonceJson.put(OPTIONS_KEY, optionsJson);

        if (authenticationInsightRequested) {
            json.put(MERCHANT_ACCOUNT_ID_KEY, merchantAccountId);
            json.put(AUTHENTICATION_INSIGHT_REQUESTED_KEY, authenticationInsightRequested);
        }
        return json;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(merchantAccountId);
        dest.writeByte(shouldValidate ? (byte) 1 : 0);
        dest.writeByte(authenticationInsightRequested ? (byte) 1 : 0);
    }

    protected Card(Parcel in) {
        super(in);
        merchantAccountId = in.readString();
        shouldValidate = in.readByte() > 0;
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