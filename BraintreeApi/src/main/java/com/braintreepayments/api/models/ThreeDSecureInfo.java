package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

/**
 * A class to contain 3D Secure information about the current
 * {@link CardNonce}
 */
public class ThreeDSecureInfo implements Parcelable {

    private static final String LIABILITY_SHIFTED_KEY = "liabilityShifted";
    private static final String LIABILITY_SHIFT_POSSIBLE_KEY = "liabilityShiftPossible";

    private boolean mLiabilityShifted;
    private boolean mLiabilityShiftPossible;

    protected static ThreeDSecureInfo fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        ThreeDSecureInfo threeDSecureInfo = new ThreeDSecureInfo();
        threeDSecureInfo.mLiabilityShifted = json.optBoolean(LIABILITY_SHIFTED_KEY);
        threeDSecureInfo.mLiabilityShiftPossible = json.optBoolean(LIABILITY_SHIFT_POSSIBLE_KEY);

        return threeDSecureInfo;
    }

    /**
     * @return If the 3D Secure liability shift has occurred for the current
     * {@link CardNonce}
     */
    public boolean isLiabilityShifted() {
        return mLiabilityShifted;
    }

    /**
     * @return If the 3D Secure liability shift is possible for the current
     * {@link CardNonce}
     */
    public boolean isLiabilityShiftPossible() {
        return mLiabilityShiftPossible;
    }

    public ThreeDSecureInfo() {}

    @Override
    public int describeContents() {
        return 0;
    }

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

        public ThreeDSecureInfo[] newArray(int size) {
            return new ThreeDSecureInfo[size];
        }
    };
}
