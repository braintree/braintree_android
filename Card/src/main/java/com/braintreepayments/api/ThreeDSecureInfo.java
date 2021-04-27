package com.braintreepayments.api;

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
    private static final String THREE_D_SECURE_AUTHENTICATION_ID_KEY = "threeDSecureAuthenticationId";
    private static final String THREE_D_SECURE_SERVER_TRANSACTION_ID_KEY = "threeDSecureServerTransactionId";
    private static final String PARES_STATUS_KEY= "paresStatus";
    private static final String AUTHENTICATION_KEY= "authentication";
    private static final String LOOKUP_KEY= "lookup";
    private static final String TRANS_STATUS_KEY= "transStatus";
    private static final String TRANS_STATUS_REASON_KEY= "transStatusReason";

    private String cavv;
    private String dsTransactionId;
    private String eciFlag;
    private String enrolled;
    private boolean liabilityShifted;
    private boolean liabilityShiftPossible;
    private String status;
    private String threeDSecureVersion;
    private boolean wasVerified;
    private String xid;
    private String acsTransactionId;
    private String threeDSecureAuthenticationId;
    private String threeDSecureServerTransactionId;
    private String paresStatus;
    private String authenticationTransactionStatus;
    private String authenticationTransactionStatusReason;
    private String lookupTransactionStatus;
    private String lookupTransactionStatusReason;

    static ThreeDSecureInfo fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        ThreeDSecureInfo threeDSecureInfo = new ThreeDSecureInfo();
        threeDSecureInfo.cavv = json.optString(CAVV_KEY);
        threeDSecureInfo.dsTransactionId = json.optString(DS_TRANSACTION_ID_KEY);
        threeDSecureInfo.eciFlag = json.optString(ECI_FLAG_KEY);
        threeDSecureInfo.enrolled = json.optString(ENROLLED_KEY);
        threeDSecureInfo.liabilityShifted = json.optBoolean(LIABILITY_SHIFTED_KEY);
        threeDSecureInfo.liabilityShiftPossible = json.optBoolean(LIABILITY_SHIFT_POSSIBLE_KEY);
        threeDSecureInfo.status = json.optString(STATUS_KEY);
        threeDSecureInfo.threeDSecureVersion = json.optString(THREE_D_SECURE_VERSION_KEY);
        threeDSecureInfo.wasVerified = json.has(LIABILITY_SHIFTED_KEY) && json.has(LIABILITY_SHIFT_POSSIBLE_KEY);
        threeDSecureInfo.xid = json.optString(XID_KEY);
        threeDSecureInfo.acsTransactionId = json.optString(ACS_TRANSACTION_ID_KEY);
        threeDSecureInfo.threeDSecureAuthenticationId = json.optString(THREE_D_SECURE_AUTHENTICATION_ID_KEY);
        threeDSecureInfo.threeDSecureServerTransactionId = json.optString(THREE_D_SECURE_SERVER_TRANSACTION_ID_KEY);
        threeDSecureInfo.paresStatus = json.optString(PARES_STATUS_KEY);

        JSONObject authenticationJson = json.optJSONObject(AUTHENTICATION_KEY);
        if (authenticationJson != null) {
            threeDSecureInfo.authenticationTransactionStatus = authenticationJson.optString(TRANS_STATUS_KEY);
            threeDSecureInfo.authenticationTransactionStatusReason = authenticationJson.optString(TRANS_STATUS_REASON_KEY);
        }

        JSONObject lookupJson = json.optJSONObject(LOOKUP_KEY);
        if (lookupJson != null) {
            threeDSecureInfo.lookupTransactionStatus = lookupJson.optString(TRANS_STATUS_KEY);
            threeDSecureInfo.lookupTransactionStatusReason = lookupJson.optString(TRANS_STATUS_REASON_KEY);
        }

        return threeDSecureInfo;
    }

    /**
     * @return Cardholder authentication verification value or "CAVV" is the main encrypted message issuers and card networks use to verify authentication has occured. Mastercard uses an "AVV" message which will also be returned in the cavv parameter.
     */
    public String getCavv() {
        return cavv;
    }

    /**
     * @return Directory Server Transaction ID is an ID used by the card brand's 3DS directory server.
     */
    public String getDsTransactionId() {
        return dsTransactionId;
    }

    /**
     * @return The ecommerce indicator flag indicates the outcome of the 3DS authentication. Possible values are 00, 01, and 02 for Mastercard 05, 06, and 07 for all other cardbrands.
     */
    public String getEciFlag() {
        return eciFlag;
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
        return enrolled;
    }

    /**
     * @return If the 3D Secure liability shift has occurred for the current
     * {@link CardNonce}
     */
    public boolean isLiabilityShifted() {
        return liabilityShifted;
    }

    /**
     * @return If the 3D Secure liability shift is possible for the current
     * {@link CardNonce}
     */
    public boolean isLiabilityShiftPossible() {
        return liabilityShiftPossible;
    }

    /**
     * @return The 3D Secure status value.
     */
    public String getStatus() {
        return status;
    }

    /**
     * @return The 3DS version used in the authentication, example "1.0.2" or "2.1.0".
     */
    public String getThreeDSecureVersion() {
        return threeDSecureVersion;
    }

    /**
     * @return If the 3D Secure lookup was performed
     */
    public boolean wasVerified() {
        return wasVerified;
    }

    /**
     * @return Transaction identifier resulting from 3D Secure authentication. Uniquely identifies the transaction and sometimes required in the authorization message. This field will no longer be used in 3DS 2 authentications.
     */
    public String getXid() {
        return xid;
    }

    /**
     * @return Unique transaction identifier assigned by the Access Control Server (ACS) to identify a single transaction.
     */
    public String getAcsTransactionId() {
        return acsTransactionId;
    }

    /**
     * @return Unique identifier assigned to the 3D Secure authentication performed for this transaction.
     */
    public String getThreeDSecureAuthenticationId() {
        return threeDSecureAuthenticationId;
    }


    /**
     * @return Unique transaction identifier assigned by the 3DS Server to identify a single transaction.
     */
    public String getThreeDSecureServerTransactionId() {
        return threeDSecureServerTransactionId;
    }

    /**
     * @return The Payer Authentication Response (PARes) Status, a transaction status result identifier. Possible Values:
     * Y – Successful Authentication
     * N – Failed Authentication
     * U – Unable to Complete Authentication
     * A – Successful Stand-In Attempts Transaction
     */
    public String getParesStatus() {
        return paresStatus;
    }

    /**
     * @return On authentication, the transaction status result identifier.
     */
    public String getAuthenticationTransactionStatus() {
        return authenticationTransactionStatus;
    }

    /**
     * @return On authentication, provides additional information as to why the transaction status has the specific value.
     */
    public String getAuthenticationTransactionStatusReason() {
        return authenticationTransactionStatusReason;
    }

    /**
     * @return On lookup, the transaction status result identifier.
     */
    public String getLookupTransactionStatus() {
        return lookupTransactionStatus;
    }

    /**
     * @return On lookup, provides additional information as to why the transaction status has the specific value.
     */
    public String getLookupTransactionStatusReason() {
        return lookupTransactionStatusReason;
    }

    public ThreeDSecureInfo() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(cavv);
        dest.writeString(dsTransactionId);
        dest.writeString(eciFlag);
        dest.writeString(enrolled);
        dest.writeByte(liabilityShifted ? (byte) 1 : (byte) 0);
        dest.writeByte(liabilityShiftPossible ? (byte) 1 : (byte) 0);
        dest.writeString(status);
        dest.writeString(threeDSecureVersion);
        dest.writeByte(wasVerified ? (byte) 1 : (byte) 0);
        dest.writeString(xid);
        dest.writeString(authenticationTransactionStatus);
        dest.writeString(authenticationTransactionStatusReason);
        dest.writeString(lookupTransactionStatus);
        dest.writeString(lookupTransactionStatusReason);
        dest.writeString(threeDSecureAuthenticationId);
    }

    private ThreeDSecureInfo(Parcel in) {
        cavv = in.readString();
        dsTransactionId = in.readString();
        eciFlag = in.readString();
        enrolled = in.readString();
        liabilityShifted = in.readByte() != 0;
        liabilityShiftPossible = in.readByte() != 0;
        status = in.readString();
        threeDSecureVersion = in.readString();
        wasVerified = in.readByte() != 0;
        xid = in.readString();
        authenticationTransactionStatus = in.readString();
        authenticationTransactionStatusReason = in.readString();
        lookupTransactionStatus = in.readString();
        lookupTransactionStatusReason = in.readString();
        threeDSecureAuthenticationId = in.readString();
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
