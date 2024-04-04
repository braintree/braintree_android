package com.braintreepayments.api.googlepay;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.core.BraintreeException;
import com.google.android.gms.common.api.Status;

/**
 * Error class thrown when a Google Pay exception is encountered.
 */
public class GooglePayException extends BraintreeException implements Parcelable {

    private final Status status;

    GooglePayException(String message, Status status) {
        super(message);
        this.status = status;
    }

    /**
     * Get the {@link Status} object that contains more details about the error and how to resolve it.
     *
     * @return {@link Status}
     */
    public Status getStatus() {
        return status;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getMessage());
        dest.writeParcelable(status, 0);
    }

    protected GooglePayException(Parcel in) {
        super(in.readString());
        status = in.readParcelable(Status.class.getClassLoader());
    }

    public static final Creator<GooglePayException> CREATOR = new Creator<GooglePayException>() {
        @Override
        public GooglePayException createFromParcel(Parcel in) {
            return new GooglePayException(in);
        }

        @Override
        public GooglePayException[] newArray(int size) {
            return new GooglePayException[size];
        }
    };
}
