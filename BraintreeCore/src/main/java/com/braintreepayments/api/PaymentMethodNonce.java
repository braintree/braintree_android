package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * Base class representing a method of payment for a customer. {@link PaymentMethodNonce} represents the
 * common interface of all payment method nonces, and can be handled by a server interchangeably.
 */
public class PaymentMethodNonce implements Parcelable {

    private final String nonce;
    private final boolean isDefault;

    PaymentMethodNonce(@NonNull String nonce, boolean isDefault) {
        this.nonce = nonce;
        this.isDefault = isDefault;
    }

    /**
     * @return The nonce generated for this payment method by the Braintree gateway. The nonce will
     *          represent this PaymentMethod for the purposes of creating transactions and other monetary
     *          actions.
     */
    @NonNull
    public String getString() {
        return nonce;
    }

    /**
     * @return {@code true} if this payment method is the default for the current customer, {@code false} otherwise
     */
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nonce);
        dest.writeByte(isDefault ? (byte) 1 : (byte) 0);
    }

    protected PaymentMethodNonce(Parcel in) {
        nonce = in.readString();
        isDefault = in.readByte() > 0;
    }

    public static final Creator<PaymentMethodNonce> CREATOR = new Creator<PaymentMethodNonce>() {
        @Override
        public PaymentMethodNonce createFromParcel(Parcel in) {
            return new PaymentMethodNonce(in);
        }

        @Override
        public PaymentMethodNonce[] newArray(int size) {
            return new PaymentMethodNonce[size];
        }
    };
}
