package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

/**
 * A class to contain 3D Secure information about the current
 * {@link CardNonce}
 */
public class ThreeDSecureInfo implements Parcelable {

    private static final String CAVV_KEY = "cavv";
    private static final String DS_TRANSACTION_ID_KEY = "dsTransactionId";
    private static final String ECI_FLAG_KEY = "eciFlag";
    private static final String ENROLLED_KEY = "enrolled";
    private static final String LIABILITY_SHIFTED_KEY = "liabilityShifted";
    private static final String LIABILITY_SHIFT_POSSIBLE_KEY = "liabilityShiftPossible";
    private static final String STATUS_KEY = "status";
    private static final String THREE_D_SECURE_VERSION_KEY = "threeDSecureVersion";
    private static final String XID_KEY = "xid";
    private static final String ACS_TRANSACTION_ID_KEY = "acsTransactionId";
    private static final String THREE_D_SECURE_SERVER_TRANSACTION_ID_KEY = "threeDSecureServerTransactionId";
    private static final String PARES_STATUS_KEY= "paresStatus";

    private String mCavv;
    private String mDsTransactionId;
    private String mEciFlag;
    private String mEnrolled;
    private boolean mLiabilityShifted;
    private boolean mLiabilityShiftPossible;
    private String mStatus;
    private String mThreeDSecureVersion;
    private boolean mWasVerified;
    private String mXid;
    private String mAcsTransactionId;
    private String mThreeDSecureServerTransactionId;
    private String mParesStatus;

    protected static ThreeDSecureInfo fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        ThreeDSecureInfo threeDSecureInfo = new ThreeDSecureInfo();
        threeDSecureInfo.mCavv = json.optString(CAVV_KEY);
        threeDSecureInfo.mDsTransactionId = json.optString(DS_TRANSACTION_ID_KEY);
        threeDSecureInfo.mEciFlag = json.optString(ECI_FLAG_KEY);
        threeDSecureInfo.mEnrolled = json.optString(ENROLLED_KEY);
        threeDSecureInfo.mLiabilityShifted = json.optBoolean(LIABILITY_SHIFTED_KEY);
        threeDSecureInfo.mLiabilityShiftPossible = json.optBoolean(LIABILITY_SHIFT_POSSIBLE_KEY);
        threeDSecureInfo.mStatus = json.optString(STATUS_KEY);
        threeDSecureInfo.mThreeDSecureVersion = json.optString(THREE_D_SECURE_VERSION_KEY);
        threeDSecureInfo.mWasVerified = json.has(LIABILITY_SHIFTED_KEY) && json.has(LIABILITY_SHIFT_POSSIBLE_KEY);
        threeDSecureInfo.mXid = json.optString(XID_KEY);
        threeDSecureInfo.mAcsTransactionId = json.optString(ACS_TRANSACTION_ID_KEY);
        threeDSecureInfo.mThreeDSecureServerTransactionId = json.optString(THREE_D_SECURE_SERVER_TRANSACTION_ID_KEY);
        threeDSecureInfo.mParesStatus = json.optString(PARES_STATUS_KEY);

        return threeDSecureInfo;
    }

    /**
     * @return Cardholder authentication verification value or "CAVV" is the main encrypted message issuers and card networks use to verify authentication has occured. Mastercard uses an "AVV" message which will also be returned in the cavv parameter.
     */
    public String getCavv() {
        return mCavv;
    }

    /**
     * @return Directory Server Transaction ID is an ID used by the card brand's 3DS directory server.
     */
    public String getDsTransactionId() {
        return mDsTransactionId;
    }

    /**
     * @return The ecommerce indicator flag indicates the outcome of the 3DS authentication. Possible values are 00, 01, and 02 for Mastercard 05, 06, and 07 for all other cardbrands.
     */
    public String getEciFlag() {
        return mEciFlag;
    }

    /**
     * @return Indicates whether a card is enrolled in a 3D Secure program or not. Possible values:
     * `Y` = Yes
     * `N` = No
     * `U` = Unavailable
     * `B` = Bypass
     * `E` = RequestFailure
     */
    public String getEnrolled() {
        return mEnrolled;
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

    /**
     * @return The 3D Secure status value.
     */
    public String getStatus() {
        return mStatus;
    }

    /**
     * @return The 3DS version used in the authentication, example "1.0.2" or "2.1.0".
     */
    public String getThreeDSecureVersion() {
        return mThreeDSecureVersion;
    }

    /**
     * @return If the 3D Secure lookup was performed
     */
    public boolean wasVerified() {
        return mWasVerified;
    }

    /**
     * @return Transaction identifier resulting from 3D Secure authentication. Uniquely identifies the transaction and sometimes required in the authorization message. This field will no longer be used in 3DS 2 authentications.
     */
    public String getXid() {
        return mXid;
    }

    /**
     * @return Unique transaction identifier assigned by the ACS to identify a single transaction.
     */
    public String getAcsTransactionId() {
        return mAcsTransactionId;
    }

    /**
     * @return Unique transaction identifier assigned by the 3DS Server to identify a single transaction.
     */
    public String getThreeDSecureServerTransactionId() {
        return mThreeDSecureServerTransactionId;
    }

    /**
     * @return Transaction status result identifier. Possible Values:
     * Y – Successful Authentication
     * N – Failed Authentication
     * U – Unable to Complete Authentication
     * A – Successful Attempts Transaction
     */
    public String getParesStatus() {
        return mParesStatus;
    }

    public ThreeDSecureInfo() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mCavv);
        dest.writeString(mDsTransactionId);
        dest.writeString(mEciFlag);
        dest.writeString(mEnrolled);
        dest.writeByte(mLiabilityShifted ? (byte) 1 : (byte) 0);
        dest.writeByte(mLiabilityShiftPossible ? (byte) 1 : (byte) 0);
        dest.writeString(mStatus);
        dest.writeString(mThreeDSecureVersion);
        dest.writeByte(mWasVerified ? (byte) 1 : (byte) 0);
        dest.writeString(mXid);
    }

    private ThreeDSecureInfo(Parcel in) {
        mCavv = in.readString();
        mDsTransactionId = in.readString();
        mEciFlag = in.readString();
        mEnrolled = in.readString();
        mLiabilityShifted = in.readByte() != 0;
        mLiabilityShiftPossible = in.readByte() != 0;
        mStatus = in.readString();
        mThreeDSecureVersion = in.readString();
        mWasVerified = in.readByte() != 0;
        mXid = in.readString();
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
