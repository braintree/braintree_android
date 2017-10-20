package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.Json;

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

    private String mErrorCode;
    private String mErrorMessage;
    private String mConversionRate;
    private String mCurrencyAmount;
    private String mCurrencyIsoCode;
    private String mRequestId;
    private String mRewardsAmount;
    private String mRewardsUnit;

    /**
     * Used to parse a response from the Braintree Gateway to be used for American Express rewards balance.
     *
     * @param jsonString The json response from the Braintree Gateway American Express rewards balance route.
     * @return The {@link com.braintreepayments.api.models.AmericanExpressRewardsBalance} with rewards balance data.
     * @throws JSONException when parsing fails.
     */
    public static AmericanExpressRewardsBalance fromJson(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);

        AmericanExpressRewardsBalance rewardsBalance = new AmericanExpressRewardsBalance();

        if (json.has(ERROR_KEY)) {
            JSONObject errorJson = json.getJSONObject(ERROR_KEY);
            rewardsBalance.mErrorMessage = errorJson.getString(ERROR_MESSAGE_KEY);
            rewardsBalance.mErrorCode = errorJson.getString(ERROR_CODE_KEY);
        }

        rewardsBalance.mConversionRate = Json.optString(json, CONVERSION_RATE_KEY, null);
        rewardsBalance.mCurrencyAmount = Json.optString(json, CURRENCY_AMOUNT_KEY, null);
        rewardsBalance.mCurrencyIsoCode = Json.optString(json, CURRENCY_ISO_CODE_KEY, null);
        rewardsBalance.mRequestId = Json.optString(json, REQUEST_ID_KEY, null);
        rewardsBalance.mRewardsAmount = Json.optString(json, REWARDS_AMOUNT_KEY, null);
        rewardsBalance.mRewardsUnit = Json.optString(json, REWARDS_UNIT_KEY, null);

        return rewardsBalance;
    }

    /**
     * @return An error code when there was an issue fetching the rewards balance
     */
    public String getErrorCode() {
        return mErrorCode;
    }

    /**
     * @return An error message when there was an issue fetching the rewards balance
     */
    public String getErrorMessage() {
        return mErrorMessage;
    }

    /**
     * @return The conversion rate associated with the rewards balance
     */
    public String getConversionRate() {
        return mConversionRate;
    }

    /**
     * @return The currency amount associated with the rewards balance
     */
    public String getCurrencyAmount() {
        return mCurrencyAmount;
    }

    /**
     * @return The currency ISO code associated with the rewards balance
     */
    public String getCurrencyIsoCode() {
        return mCurrencyIsoCode;
    }

    /**
     * @return The request ID used when fetching the rewards balance
     */
    public String getRequestId() {
        return mRequestId;
    }

    /**
     * @return The rewards amount associated with the rewards balance
     */
    public String getRewardsAmount() {
        return mRewardsAmount;
    }

    /**
     * @return The rewards unit associated with the rewards balance
     */
    public String getRewardsUnit() {
        return mRewardsUnit;
    }

    public AmericanExpressRewardsBalance() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mErrorCode);
        dest.writeString(mErrorMessage);
        dest.writeString(mConversionRate);
        dest.writeString(mCurrencyAmount);
        dest.writeString(mCurrencyIsoCode);
        dest.writeString(mRequestId);
        dest.writeString(mRewardsAmount);
        dest.writeString(mRewardsUnit);
    }

    private AmericanExpressRewardsBalance(Parcel in) {
        mErrorCode = in.readString();
        mErrorMessage = in.readString();
        mConversionRate = in.readString();
        mCurrencyAmount = in.readString();
        mCurrencyIsoCode = in.readString();
        mRequestId = in.readString();
        mRewardsAmount = in.readString();
        mRewardsUnit = in.readString();
    }

    public static final Creator<AmericanExpressRewardsBalance> CREATOR = new Creator<AmericanExpressRewardsBalance>() {
        public AmericanExpressRewardsBalance createFromParcel(Parcel source) {
            return new AmericanExpressRewardsBalance(source);
        }

        public AmericanExpressRewardsBalance[] newArray(int size) {
            return new AmericanExpressRewardsBalance[size];
        }
    };
}
