package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

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

    private boolean mCardAmountImmutable;
    private PayPalCreditFinancingAmount mMonthlyPayment;
    private boolean mPayerAcceptance;
    private int mTerm;
    private PayPalCreditFinancingAmount mTotalCost;
    private PayPalCreditFinancingAmount mTotalInterest;

    private PayPalCreditFinancing() {}

    public static PayPalCreditFinancing fromJson(JSONObject creditFinancing) throws JSONException {
        PayPalCreditFinancing result = new PayPalCreditFinancing();

        if (creditFinancing == null) {
            return result;
        }

        result.mCardAmountImmutable = creditFinancing.optBoolean(CARD_AMOUNT_IMMUTABLE_KEY, false);
        result.mMonthlyPayment = PayPalCreditFinancingAmount.fromJson(creditFinancing.getJSONObject(MONTHLY_PAYMENT_KEY));
        result.mPayerAcceptance = creditFinancing.optBoolean(PAYER_ACCEPTANCE_KEY, false);
        result.mTerm = creditFinancing.optInt(TERM_KEY, 0);
        result.mTotalCost = PayPalCreditFinancingAmount.fromJson(creditFinancing.getJSONObject(TOTAL_COST_KEY));
        result.mTotalInterest = PayPalCreditFinancingAmount.fromJson(creditFinancing.getJSONObject(TOTAL_INTEREST_KEY));

        return result;
    }

    /**
     * @return Length of financing terms in months.
     */
    public int getTerm() {
        return mTerm;
    }

    /**
     * @return Indicates whether the card amount is editable after payer's acceptance on PayPal side.
     */
    public boolean isCardAmountImmutable() {
        return mCardAmountImmutable;
    }

    /**
     * @return Estimated amount per month that the customer will need to pay including fees and interest.
     */
    public PayPalCreditFinancingAmount getMonthlyPayment() {
        return mMonthlyPayment;
    }

    /**
     * @return Status of whether the customer ultimately was approved for and chose to make the payment using the approved installment credit.
     */
    public boolean hasPayerAcceptance() {
        return mPayerAcceptance;
    }

    /**
     * @return Estimated total payment amount including interest and fees the user will pay during the lifetime of the loan.
     */
    public PayPalCreditFinancingAmount getTotalCost() {
        return mTotalCost;
    }

    /**
     * @return Estimated interest or fees amount the payer will have to pay during the lifetime of the loan.
     */
    public PayPalCreditFinancingAmount getTotalInterest() {
        return mTotalInterest;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(mCardAmountImmutable? (byte) 1:0);
        dest.writeParcelable(mMonthlyPayment, flags);
        dest.writeByte(mPayerAcceptance? (byte) 1:0);
        dest.writeInt(mTerm);
        dest.writeParcelable(mTotalCost, flags);
        dest.writeParcelable(mTotalInterest, flags);
    }

    private PayPalCreditFinancing(Parcel in) {
        mCardAmountImmutable = in.readByte() != 0;
        mMonthlyPayment = in.readParcelable(PayPalCreditFinancingAmount.class.getClassLoader());
        mPayerAcceptance = in.readByte() != 0;
        mTerm = in.readInt();
        mTotalCost = in.readParcelable(PayPalCreditFinancingAmount.class.getClassLoader());
        mTotalInterest = in.readParcelable(PayPalCreditFinancingAmount.class.getClassLoader());
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
