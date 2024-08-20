package com.braintreepayments.api.threedsecure

import androidx.annotation.RestrictTo
import com.braintreepayments.api.card.AuthenticationInsight
import com.braintreepayments.api.card.BinData
import com.braintreepayments.api.card.CardNonce
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * [PaymentMethodNonce] representing a [cardNonce] that includes [ThreeDSecureInfo]
 *
 * @property threeDSecureInfo The 3D Secure info for the current [ThreeDSecureNonce] or `null`
 */
@Parcelize
data class ThreeDSecureNonce internal constructor(
    override val cardType: String,
    override val lastTwo: String,
    override val lastFour: String,
    override val bin: String,
    override val binData: BinData,
    override val authenticationInsight: AuthenticationInsight?,
    override val expirationMonth: String,
    override val expirationYear: String,
    override val cardholderName: String,
    override val string: String,
    override val isDefault: Boolean,
    val threeDSecureInfo: ThreeDSecureInfo
) : CardNonce(
    cardType = cardType,
    lastTwo = lastTwo,
    lastFour = lastFour,
    bin = bin,
    binData = binData,
    authenticationInsight = authenticationInsight,
    expirationMonth = expirationMonth,
    expirationYear = expirationYear,
    cardholderName = cardholderName,
    string = string,
    isDefault = isDefault
) {

    companion object {
        private const val THREE_D_SECURE_INFO_KEY = "threeDSecureInfo"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Throws(JSONException::class)
        @JvmStatic
        fun fromJSON(json: JSONObject): ThreeDSecureNonce {
            val cardNonce = CardNonce.fromJSON(json)
            val threeDSecureInfo: ThreeDSecureInfo
            if (json.has(DATA_KEY)) { // graphQL
                threeDSecureInfo = ThreeDSecureInfo.fromJson(null)
            } else if (json.has(API_RESOURCE_KEY)) { // REST
                val json = json.getJSONArray(API_RESOURCE_KEY).getJSONObject(0)
                threeDSecureInfo =
                    ThreeDSecureInfo.fromJson(json.optJSONObject(THREE_D_SECURE_INFO_KEY))
            } else { // plain JSON
                threeDSecureInfo = ThreeDSecureInfo.fromJson(json.optJSONObject(THREE_D_SECURE_INFO_KEY))
            }
            return ThreeDSecureNonce(
                cardType = cardNonce.cardType,
                lastTwo = cardNonce.lastTwo,
                lastFour = cardNonce.lastFour,
                bin = cardNonce.bin,
                binData = cardNonce.binData,
                authenticationInsight = cardNonce.authenticationInsight,
                expirationMonth = cardNonce.expirationMonth,
                expirationYear = cardNonce.expirationYear,
                cardholderName = cardNonce.cardholderName,
                string = cardNonce.string,
                isDefault = cardNonce.isDefault,
                threeDSecureInfo = threeDSecureInfo
            )
        }
    }
}
