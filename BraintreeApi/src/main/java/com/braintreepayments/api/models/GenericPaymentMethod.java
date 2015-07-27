package com.braintreepayments.api.models;

import android.os.Parcel;

public class GenericPaymentMethod extends PaymentMethod implements android.os.Parcelable {

    public GenericPaymentMethod(String nonce) {
        mNonce = nonce;
    }

    @Override
    public String getTypeLabel() {
        return null;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mNonce);
        dest.writeString(mDescription);
    }

    public GenericPaymentMethod() {}

    private GenericPaymentMethod(Parcel in) {
        mNonce = in.readString();
        mDescription = in.readString();
    }

    public static final Creator<GenericPaymentMethod> CREATOR = new Creator<GenericPaymentMethod>() {
        public GenericPaymentMethod createFromParcel(Parcel source) {
            return new GenericPaymentMethod(source);
        }

        public GenericPaymentMethod[] newArray(int size) {
            return new GenericPaymentMethod[size];
        }
    };
}
