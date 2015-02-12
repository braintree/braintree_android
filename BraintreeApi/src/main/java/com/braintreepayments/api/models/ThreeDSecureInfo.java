package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.annotations.Beta;

/**
 * A class to contain 3D Secure information about the current
 * {@link com.braintreepayments.api.models.Card}
 */
@Beta
public class ThreeDSecureInfo implements Parcelable {

    private boolean liabilityShifted;
    private boolean liabilityShiftPossible;

    public ThreeDSecureInfo() {}

    /**
     * @return If the 3D Secure liability shift has occurred for the current
     * {@link com.braintreepayments.api.models.Card}
     */
    public boolean isLiabilityShifted() {
        return liabilityShifted;
    }

    /**
     * @return If the 3D Secure liability shift is possible for the current
     * {@link com.braintreepayments.api.models.Card}
     */
    public boolean isLiabilityShiftPossible() {
        return liabilityShiftPossible;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(liabilityShifted ? (byte) 1 : (byte) 0);
        dest.writeByte(liabilityShiftPossible ? (byte) 1 : (byte) 0);
    }

    private ThreeDSecureInfo(Parcel in) {
        this.liabilityShifted = in.readByte() != 0;
        this.liabilityShiftPossible = in.readByte() != 0;
    }

    public static final Creator<ThreeDSecureInfo> CREATOR = new Creator<ThreeDSecureInfo>() {
        public ThreeDSecureInfo createFromParcel(Parcel source) {
            return new ThreeDSecureInfo(source);
        }

        public ThreeDSecureInfo[] newArray(int size) {return new ThreeDSecureInfo[size];}
    };
}

