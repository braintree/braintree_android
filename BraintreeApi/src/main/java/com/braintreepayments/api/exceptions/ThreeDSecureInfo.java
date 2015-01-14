package com.braintreepayments.api.exceptions;

import android.os.Parcel;

public class ThreeDSecureInfo extends ErrorInfo implements android.os.Parcelable {

    private boolean liabilityShifted;
    private boolean liabilityShiftPossible;

    public ThreeDSecureInfo() {}

    public boolean isLiabilityShifted() {
        return liabilityShifted;
    }

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
