package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

public class PayPalProductAttributes implements Parcelable {

    // TODO: - add docstrings once we get param info from PP

    private String mChargePattern;
    private String mName;
    private String mProductCode;

    public PayPalProductAttributes() { }

    public PayPalProductAttributes chargePattern(String chargePattern) {
        mChargePattern = chargePattern;
        return this;
    }

    public PayPalProductAttributes name(String name) {
        mName = name;
        return this;
    }

    public PayPalProductAttributes productCode(String productCode) {
        mProductCode = productCode;
        return this;
    }

    public String getName() {
        return mName;
    }

    public String getProductCode() {
        return mProductCode;
    }

    public String getChargePattern() {
        return mChargePattern;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mChargePattern);
        dest.writeString(mName);
        dest.writeString(mProductCode);
    }

    private PayPalProductAttributes(Parcel in) {
        mChargePattern = in.readString();
        mName = in.readString();
        mProductCode = in.readString();
    }

    public static final Creator<PayPalProductAttributes> CREATOR = new Creator<PayPalProductAttributes>() {
        public PayPalProductAttributes createFromParcel(Parcel source) {
            return new PayPalProductAttributes(source);
        }

        public PayPalProductAttributes[] newArray(int size) {
            return new PayPalProductAttributes[size];
        }
    };
}
