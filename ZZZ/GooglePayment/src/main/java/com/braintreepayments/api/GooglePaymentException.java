package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.google.android.gms.common.api.Status;

/**
 * Error class thrown when a Google Payment exception is encountered.
 */
public class GooglePaymentException extends BraintreeException implements Parcelable {

    private Status mStatus;

    public GooglePaymentException(String message, Status status) {
        super(message);
        mStatus = status;
    }

    /**
     * Get the {@link Status} object that contains more details about the error and how to resolve it.
     *
     * @return {@link Status}
     */
    public Status getStatus() {
        return mStatus;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getMessage());
        dest.writeParcelable(mStatus, 0);
    }

    protected GooglePaymentException(Parcel in) {
        super(in.readString());
        mStatus = in.readParcelable(Status.class.getClassLoader());
    }

    public static final Creator<GooglePaymentException> CREATOR = new Creator<GooglePaymentException>() {
        @Override
        public GooglePaymentException createFromParcel(Parcel in) {
            return new GooglePaymentException(in);
        }

        @Override
        public GooglePaymentException[] newArray(int size) {
            return new GooglePaymentException[size];
        }
    };
}
