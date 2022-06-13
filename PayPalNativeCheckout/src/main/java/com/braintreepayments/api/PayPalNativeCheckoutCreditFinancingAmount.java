package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * The currency and amount in a PayPal credit financing response
 */
public class PayPalNativeCheckoutCreditFinancingAmount implements Parcelable {

    private static final String CURRENCY_KEY = "currency";
    private static final String VALUE_KEY = "value";

    private String currency;
    private String value;

    private PayPalNativeCheckoutCreditFinancingAmount() {}

    static PayPalNativeCheckoutCreditFinancingAmount fromJson(JSONObject amount) {
        PayPalNativeCheckoutCreditFinancingAmount result = new PayPalNativeCheckoutCreditFinancingAmount();

        if (amount == null) {
            return result;
        }

        result.currency = Json.optString(amount, CURRENCY_KEY, null);
        result.value = Json.optString(amount, VALUE_KEY, null);

        return result;
    }

    /**
     * @return 3 letter currency code as defined by <a href="http://www.iso.org/iso/home/standards/currency_codes.htm">ISO 4217</a>.
     */
    @Nullable
    public String getCurrency() {
        return currency;
    }

    /**
     * @return An amount defined by <a href="http://www.iso.org/iso/home/standards/currency_codes.htm">ISO 4217</a> for the given currency.
     */
    @Nullable
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%s %s", value, currency);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(currency);
        dest.writeString(value);
    }

    private PayPalNativeCheckoutCreditFinancingAmount(Parcel in) {
        currency = in.readString();
        value = in.readString();
    }

    public static final Creator<PayPalNativeCheckoutCreditFinancingAmount> CREATOR = new Creator<PayPalNativeCheckoutCreditFinancingAmount>() {
        public PayPalNativeCheckoutCreditFinancingAmount createFromParcel(Parcel source) {
            return new PayPalNativeCheckoutCreditFinancingAmount(source);
        }

        public PayPalNativeCheckoutCreditFinancingAmount[] newArray(int size) {
            return new PayPalNativeCheckoutCreditFinancingAmount[size];
        }
    };
}
