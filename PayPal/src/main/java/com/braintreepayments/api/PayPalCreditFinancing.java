package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the PayPal credit financing response.
 */
public class PayPalCreditFinancing implements Parcelable {

    private static final String CARD_AMOUNT_IMMUTABLE_KEY = "cardAmountImmutable";
    private static final String MONTHLY_PAYMENT_KEY = "monthlyPayment";
    private static final String PAYER_ACCEPTANCE_KEY = "payerAcceptance";
    private static final String TERM_KEY = "term";
    private static final String TOTAL_COST_KEY = "totalCost";
    private static final String TOTAL_INTEREST_KEY = "totalInterest";

    private boolean cardAmountImmutable;
    private PayPalCreditFinancingAmount monthlyPayment;
    private boolean payerAcceptance;
    private int term;
    private PayPalCreditFinancingAmount totalCost;
    private PayPalCreditFinancingAmount totalInterest;

    private PayPalCreditFinancing() {}

    static PayPalCreditFinancing fromJson(JSONObject creditFinancing) throws JSONException {
        PayPalCreditFinancing result = new PayPalCreditFinancing();

        if (creditFinancing == null) {
            return result;
        }

        result.cardAmountImmutable = creditFinancing.optBoolean(CARD_AMOUNT_IMMUTABLE_KEY, false);
        result.monthlyPayment = PayPalCreditFinancingAmount.fromJson(creditFinancing.getJSONObject(MONTHLY_PAYMENT_KEY));
        result.payerAcceptance = creditFinancing.optBoolean(PAYER_ACCEPTANCE_KEY, false);
        result.term = creditFinancing.optInt(TERM_KEY, 0);
        result.totalCost = PayPalCreditFinancingAmount.fromJson(creditFinancing.getJSONObject(TOTAL_COST_KEY));
        result.totalInterest = PayPalCreditFinancingAmount.fromJson(creditFinancing.getJSONObject(TOTAL_INTEREST_KEY));

        return result;
    }

    /**
     * @return Length of financing terms in months.
     */
    public int getTerm() {
        return term;
    }

    /**
     * @return Indicates whether the card amount is editable after payer's acceptance on PayPal side.
     */
    public boolean isCardAmountImmutable() {
        return cardAmountImmutable;
    }

    /**
     * @return Estimated amount per month that the customer will need to pay including fees and interest.
     */
    @NonNull
    public PayPalCreditFinancingAmount getMonthlyPayment() {
        return monthlyPayment;
    }

    /**
     * @return Status of whether the customer ultimately was approved for and chose to make the payment using the approved installment credit.
     */
    public boolean hasPayerAcceptance() {
        return payerAcceptance;
    }

    /**
     * @return Estimated total payment amount including interest and fees the user will pay during the lifetime of the loan.
     */
    @NonNull
    public PayPalCreditFinancingAmount getTotalCost() {
        return totalCost;
    }

    /**
     * @return Estimated interest or fees amount the payer will have to pay during the lifetime of the loan.
     */
    @NonNull
    public PayPalCreditFinancingAmount getTotalInterest() {
        return totalInterest;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(cardAmountImmutable ? (byte) 1:0);
        dest.writeParcelable(monthlyPayment, flags);
        dest.writeByte(payerAcceptance ? (byte) 1:0);
        dest.writeInt(term);
        dest.writeParcelable(totalCost, flags);
        dest.writeParcelable(totalInterest, flags);
    }

    private PayPalCreditFinancing(Parcel in) {
        cardAmountImmutable = in.readByte() != 0;
        monthlyPayment = in.readParcelable(PayPalCreditFinancingAmount.class.getClassLoader());
        payerAcceptance = in.readByte() != 0;
        term = in.readInt();
        totalCost = in.readParcelable(PayPalCreditFinancingAmount.class.getClassLoader());
        totalInterest = in.readParcelable(PayPalCreditFinancingAmount.class.getClassLoader());
    }

    public static final Creator<PayPalCreditFinancing> CREATOR = new Creator<PayPalCreditFinancing>() {
        public PayPalCreditFinancing createFromParcel(Parcel source) {
            return new PayPalCreditFinancing(source);
        }

        public PayPalCreditFinancing[] newArray(int size) {
            return new PayPalCreditFinancing[size];
        }
    };
}
