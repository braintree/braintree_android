package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.annotations.Beta;
import com.google.gson.annotations.SerializedName;

/**
 * A class to contain 3D Secure information about the current
 * {@link com.braintreepayments.api.models.Card}
 */
@Beta
public class ThreeDSecureInfo implements Parcelable {

    @SerializedName("liabilityShifted") private boolean mLiabilityShifted;
    @SerializedName("liabilityShiftPossible") private boolean mLiabilityShiftPossible;

    public ThreeDSecureInfo() {}

    /**
     * @return If the 3D Secure liability shift has occurred for the current
     * {@link com.braintreepayments.api.models.Card}
     */
    public boolean isLiabilityShifted() {
        return mLiabilityShifted;
    }

    /**
     * @return If the 3D Secure liability shift is possible for the current
     * {@link com.braintreepayments.api.models.Card}
     */
    public boolean isLiabilityShiftPossible() {
        return mLiabilityShiftPossible;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(mLiabilityShifted ? (byte) 1 : (byte) 0);
        dest.writeByte(mLiabilityShiftPossible ? (byte) 1 : (byte) 0);
    }

    private ThreeDSecureInfo(Parcel in) {
        mLiabilityShifted = in.readByte() != 0;
        mLiabilityShiftPossible = in.readByte() != 0;
    }

    public static final Creator<ThreeDSecureInfo> CREATOR = new Creator<ThreeDSecureInfo>() {
        public ThreeDSecureInfo createFromParcel(Parcel source) {
            return new ThreeDSecureInfo(source);
        }

        public ThreeDSecureInfo[] newArray(int size) {return new ThreeDSecureInfo[size];}
    };
}

