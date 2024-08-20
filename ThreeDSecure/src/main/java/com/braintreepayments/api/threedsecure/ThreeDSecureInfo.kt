package com.braintreepayments.api.threedsecure

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * A class to contain 3D Secure information about the current
 *
 * @property cavv Cardholder authentication verification value or "CAVV" is the main encrypted
 * message issuers and card networks use to verify authentication has occured. Mastercard uses an
 * "AVV" message which will also be returned in the cavv parameter.
 * @property dsTransactionId Directory Server Transaction ID is an ID used by the card brand's 3DS
 * directory server.
 * @property eciFlag The ecommerce indicator flag indicates the outcome of the 3DS authentication.
 * Possible values are 00, 01, and 02 for Mastercard 05, 06, and 07 for all other cardbrands.
 * @property enrolled Indicates whether a card is enrolled in a 3D Secure program or not.
 * Possible values:
 * `Y` = Yes
 * `N` = No
 * `U` = Unavailable
 * `B` = Bypass
 * `E` = RequestFailure
 * @property liabilityShifted If the 3D Secure liability shift has occurred for the current
 * @property liabilityShiftPossible If the 3D Secure liability shift is possible for the current
 * @property status The 3D Secure status value.
 * @property threeDSecureVersion The 3DS version used in the authentication, example "1.0.2" or
 * "2.1.0".
 * @property wasVerified If the 3D Secure lookup was performed
 * @property xid Transaction identifier resulting from 3D Secure authentication. Uniquely identifies
 * the transaction and sometimes required in the authorization message. This field will no longer be
 * used in 3DS 2 authentications.
 * @property acsTransactionId Unique transaction identifier assigned by the Access Control Server
 * (ACS) to identify a single transaction.
 * @property threeDSecureAuthenticationId Unique identifier assigned to the 3D Secure authentication
 * performed for this transaction.
 * @property threeDSecureServerTransactionId Unique transaction identifier assigned by the 3DS
 * Server to identify a single transaction.
 * @property paresStatus The Payer Authentication Response (PARes) Status, a transaction status result identifier. Possible Values:
 * Y – Successful Authentication
 * N – Failed Authentication
 * U – Unable to Complete Authentication
 * A – Successful Stand-In Attempts Transaction
 * @property authenticationTransactionStatus On authentication, the transaction status result
 * identifier.
 * @property authenticationTransactionStatusReason On authentication, provides additional
 * information as to why the transaction status has the specific value.
 * @property lookupTransactionStatus On lookup, the transaction status result identifier.
 * @property lookupTransactionStatusReason On lookup, provides additional information as to why the
 * transaction status has the specific value.
 */
@Parcelize
data class ThreeDSecureInfo internal constructor(
    val cavv: String? = null,
    val dsTransactionId: String? = null,
    val eciFlag: String? = null,
    val enrolled: String? = null,
    val liabilityShifted: Boolean = false,
    val liabilityShiftPossible: Boolean = false,
    val status: String? = null,
    val threeDSecureVersion: String? = null,
    val wasVerified: Boolean = false,
    val xid: String? = null,
    val acsTransactionId: String? = null,
    val threeDSecureAuthenticationId: String? = null,
    val threeDSecureServerTransactionId: String? = null,
    val paresStatus: String? = null,
    val authenticationTransactionStatus: String? = null,
    val authenticationTransactionStatusReason: String? = null,
    val lookupTransactionStatus: String? = null,
    val lookupTransactionStatusReason: String? = null
) : Parcelable {

    companion object {
        private const val CAVV_KEY = "cavv"
        private const val DS_TRANSACTION_ID_KEY = "dsTransactionId"
        private const val ECI_FLAG_KEY = "eciFlag"
        private const val ENROLLED_KEY = "enrolled"
        private const val LIABILITY_SHIFTED_KEY = "liabilityShifted"
        private const val LIABILITY_SHIFT_POSSIBLE_KEY = "liabilityShiftPossible"
        private const val STATUS_KEY = "status"
        private const val THREE_D_SECURE_VERSION_KEY = "threeDSecureVersion"
        private const val XID_KEY = "xid"
        private const val ACS_TRANSACTION_ID_KEY = "acsTransactionId"
        private const val THREE_D_SECURE_AUTHENTICATION_ID_KEY = "threeDSecureAuthenticationId"
        private const val THREE_D_SECURE_SERVER_TRANSACTION_ID_KEY =
            "threeDSecureServerTransactionId"
        private const val PARES_STATUS_KEY = "paresStatus"
        private const val AUTHENTICATION_KEY = "authentication"
        private const val LOOKUP_KEY = "lookup"
        private const val TRANS_STATUS_KEY = "transStatus"
        private const val TRANS_STATUS_REASON_KEY = "transStatusReason"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        fun fromJson(json: JSONObject?): ThreeDSecureInfo {
            val jsonObject = json ?: JSONObject()

            val authenticationJson = jsonObject.optJSONObject(AUTHENTICATION_KEY)
            val lookupJson = jsonObject.optJSONObject(LOOKUP_KEY)

            return ThreeDSecureInfo(
                cavv = jsonObject.optString(CAVV_KEY),
                dsTransactionId = jsonObject.optString(DS_TRANSACTION_ID_KEY),
                eciFlag = jsonObject.optString(ECI_FLAG_KEY),
                enrolled = jsonObject.optString(ENROLLED_KEY),
                liabilityShifted = jsonObject.optBoolean(LIABILITY_SHIFTED_KEY),
                liabilityShiftPossible = jsonObject.optBoolean(LIABILITY_SHIFT_POSSIBLE_KEY),
                status = jsonObject.optString(STATUS_KEY),
                threeDSecureVersion = jsonObject.optString(THREE_D_SECURE_VERSION_KEY),
                wasVerified = jsonObject.has(LIABILITY_SHIFTED_KEY) && jsonObject.has(
                    LIABILITY_SHIFT_POSSIBLE_KEY
                ),
                xid = jsonObject.optString(XID_KEY),
                acsTransactionId = jsonObject.optString(ACS_TRANSACTION_ID_KEY),
                threeDSecureAuthenticationId = jsonObject.optString(
                    THREE_D_SECURE_AUTHENTICATION_ID_KEY
                ),
                threeDSecureServerTransactionId = jsonObject.optString(
                    THREE_D_SECURE_SERVER_TRANSACTION_ID_KEY
                ),
                paresStatus = jsonObject.optString(PARES_STATUS_KEY),
                authenticationTransactionStatus = authenticationJson?.optString(TRANS_STATUS_KEY),
                authenticationTransactionStatusReason = authenticationJson?.optString(
                    TRANS_STATUS_REASON_KEY
                ),
                lookupTransactionStatus = lookupJson?.optString(TRANS_STATUS_KEY),
                lookupTransactionStatusReason = lookupJson?.optString(TRANS_STATUS_REASON_KEY)
            )
        }
    }
}