package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Generic object whose subclasses will be the results of BraintreePayments
 */
public class BraintreePaymentResult implements Parcelable {

    public BraintreePaymentResult() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {}

    protected BraintreePaymentResult(Parcel in) {}

    public static final Creator<BraintreePaymentResult> CREATOR = new Creator<BraintreePaymentResult>() {
        @Override
        public BraintreePaymentResult createFromParcel(Parcel in) {
            return new BraintreePaymentResult(in);
        }

        @Override
        public BraintreePaymentResult[] newArray(int size) {
            return new BraintreePaymentResult[size];
        }
    };
}
