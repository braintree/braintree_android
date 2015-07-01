package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Additional processing options for creating a {@link com.braintreepayments.api.models.PaymentMethod}
 * in the Braintree gateway.
 */
public class PaymentMethodOptions implements Parcelable {

    @SerializedName("validate") private boolean mValidate;

    public void setValidate(boolean validate) {
        mValidate = validate;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(mValidate ? (byte) 1 : (byte) 0);
    }

    public PaymentMethodOptions() {}

    protected PaymentMethodOptions(Parcel in) {
        mValidate = in.readByte() != 0;
    }

    public static final Creator<PaymentMethodOptions> CREATOR = new Creator<PaymentMethodOptions>() {
        public PaymentMethodOptions createFromParcel(Parcel source) {
            return new PaymentMethodOptions(source);
        }

        public PaymentMethodOptions[] newArray(int size) {
            return new PaymentMethodOptions[size];
        }
    };
}
