package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to parse and contain American Express rewards balance
 */
public class AmericanExpressRewardsBalance implements Parcelable {

    private static final String ERROR_KEY = "error";
    private static final String ERROR_CODE_KEY = "code";
    private static final String ERROR_MESSAGE_KEY = "message";
    private static final String CONVERSION_RATE_KEY = "conversionRate";
    private static final String CURRENCY_AMOUNT_KEY = "currencyAmount";
    private static final String CURRENCY_ISO_CODE_KEY = "currencyIsoCode";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String REWARDS_AMOUNT_KEY = "rewardsAmount";
    private static final String REWARDS_UNIT_KEY = "rewardsUnit";

    private String errorCode;
    private String errorMessage;
    private String conversionRate;
    private String currencyAmount;
    private String currencyIsoCode;
    private String requestId;
    private String rewardsAmount;
    private String rewardsUnit;

    /**
     * Used to parse a response from the Braintree Gateway to be used for American Express rewards balance.
     *
     * @param jsonString The json response from the Braintree Gateway American Express rewards balance route.
     * @return The {@link AmericanExpressRewardsBalance} with rewards balance data.
     * @throws JSONException when parsing fails.
     */
    static AmericanExpressRewardsBalance fromJson(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);

        AmericanExpressRewardsBalance rewardsBalance = new AmericanExpressRewardsBalance();

        if (json.has(ERROR_KEY)) {
            JSONObject errorJson = json.getJSONObject(ERROR_KEY);
            rewardsBalance.errorMessage = errorJson.getString(ERROR_MESSAGE_KEY);
            rewardsBalance.errorCode = errorJson.getString(ERROR_CODE_KEY);
        }

        rewardsBalance.conversionRate = Json.optString(json, CONVERSION_RATE_KEY, null);
        rewardsBalance.currencyAmount = Json.optString(json, CURRENCY_AMOUNT_KEY, null);
        rewardsBalance.currencyIsoCode = Json.optString(json, CURRENCY_ISO_CODE_KEY, null);
        rewardsBalance.requestId = Json.optString(json, REQUEST_ID_KEY, null);
        rewardsBalance.rewardsAmount = Json.optString(json, REWARDS_AMOUNT_KEY, null);
        rewardsBalance.rewardsUnit = Json.optString(json, REWARDS_UNIT_KEY, null);

        return rewardsBalance;
    }

    /**
     * @return An error code when there was an issue fetching the rewards balance
     */
    @Nullable
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * @return An error message when there was an issue fetching the rewards balance
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return The conversion rate associated with the rewards balance
     */
    @Nullable
    public String getConversionRate() {
        return conversionRate;
    }

    /**
     * @return The currency amount associated with the rewards balance
     */
    @Nullable
    public String getCurrencyAmount() {
        return currencyAmount;
    }

    /**
     * @return The currency ISO code associated with the rewards balance
     */
    @Nullable
    public String getCurrencyIsoCode() {
        return currencyIsoCode;
    }

    /**
     * @return The request ID used when fetching the rewards balance
     */
    @Nullable
    public String getRequestId() {
        return requestId;
    }

    /**
     * @return The rewards amount associated with the rewards balance
     */
    @Nullable
    public String getRewardsAmount() {
        return rewardsAmount;
    }

    /**
     * @return The rewards unit associated with the rewards balance
     */
    @Nullable
    public String getRewardsUnit() {
        return rewardsUnit;
    }

    private AmericanExpressRewardsBalance() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(errorCode);
        dest.writeString(errorMessage);
        dest.writeString(conversionRate);
        dest.writeString(currencyAmount);
        dest.writeString(currencyIsoCode);
        dest.writeString(requestId);
        dest.writeString(rewardsAmount);
        dest.writeString(rewardsUnit);
    }

    private AmericanExpressRewardsBalance(Parcel in) {
        errorCode = in.readString();
        errorMessage = in.readString();
        conversionRate = in.readString();
        currencyAmount = in.readString();
        currencyIsoCode = in.readString();
        requestId = in.readString();
        rewardsAmount = in.readString();
        rewardsUnit = in.readString();
    }

    public static final Creator<AmericanExpressRewardsBalance> CREATOR =
        new Creator<AmericanExpressRewardsBalance>() {
            public AmericanExpressRewardsBalance createFromParcel(Parcel source) {
                return new AmericanExpressRewardsBalance(source);
            }

            public AmericanExpressRewardsBalance[] newArray(int size) {
                return new AmericanExpressRewardsBalance[size];
            }
        };
}
