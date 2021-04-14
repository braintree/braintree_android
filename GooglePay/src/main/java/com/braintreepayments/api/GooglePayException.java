package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.common.api.Status;

/**
 * Error class thrown when a Google Pay exception is encountered.
 */
public class GooglePayException extends BraintreeException implements Parcelable {

    private Status mStatus;

    GooglePayException(String message, Status status) {
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

    protected GooglePayException(Parcel in) {
        super(in.readString());
        mStatus = in.readParcelable(Status.class.getClassLoader());
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
