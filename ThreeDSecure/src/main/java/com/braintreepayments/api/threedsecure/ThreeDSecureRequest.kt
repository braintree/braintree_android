package com.braintreepayments.api.threedsecure

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * A class to contain 3D Secure request information used for authentication
 *
 * @property nonce The nonce that represents a card to perform a 3D Secure verification against.
 * @property amount The amount of the transaction in the current merchant account'scurrency.
 * This must be expressed in numbers with an optional decimal (using `.`) and precision up to the
 * hundredths place. For example, if you're processing a transaction for 1.234,56 € then `amount`
 * should be `1234.56`.
 * @property mobilePhoneNumber Optional. The mobile phone number used for verification. Only
 * numbers. Remove dashes, parentheses and other characters.
 * @property email Optional. The email used for verification.
 * @property shippingMethod Optional. The 2-digit string indicating the shipping method chosen for
 * the transaction.
 * Possible Values: 01 Same Day 02 Overnight / Expedited 03 Priority (2-3 Days) 04 Ground 05
 * Electronic Delivery 06 Ship to Store
 * @property billingAddress Optional. The billing address used for verification.
 * @property accountType Optional. The account type selected by the cardholder. Some cards can be
 * processed using either a credit or debit account and cardholders have the option to choose which
 * account to use.
 * @property additionalInformation Optional. Additional information used for verification.
 * @property challengeRequested Optional. If set to true, the customer will be asked to complete the
 * authentication challenge if possible.
 * @property dataOnlyRequested If data was only requested
 * @property exemptionRequested Optional. If set to true, an exemption to the authentication
 * challenge will be requested.
 * @property requestedExemptionType Optional. 3D Secure requested exemption type. If an exemption
 * is requested and the exemption's conditions are satisfied, then it will be applied.
 * @property cardAddChallengeRequested Optional. An authentication created using this flag should
 * only be used for adding a payment method to the merchant's vault and not for creating
 * transactions.
 * @property v2UiCustomization Optional. UI Customization for the 3DS2 challenge views.
 * @property uiType Optional. Sets all UI types that the device supports for displaying specific
 * challenge user interfaces in the 3D Secure challenge. Possible Values: 01 BOTH 02 Native 03 HTML
 * Defaults to BOTH
 * @property renderTypes Optional. List of all the render types that the device supports for
 * displaying specific challenge user interfaces within the 3D Secure challenge. When using
 * `ThreeDSecureUIType.BOTH` or `ThreeDSecureUIType.HTML`, all `ThreeDSecureRenderType` options must
 * be set. When using `ThreeDSecureUIType.NATIVE`, all `ThreeDSecureRenderType` options except
 * `ThreeDSecureRenderType.RENDER_HTML` must be set. Defaults to OTP, OOB, SINGLE_SELECT,
 * MULTI_SELECT, RENDER_HTML
 * @property customFields Optional. Object where each key is the name of a custom field which has
 * been configured in the Control Panel. In the Control Panel you can configure 3D Secure Rules
 * which trigger on certain values.
 */
@Parcelize
data class ThreeDSecureRequest(
    var nonce: String? = null,
    var amount: String? = null,
    var mobilePhoneNumber: String? = null,
    var email: String? = null,
    var shippingMethod: ThreeDSecureShippingMethod? = null,
    var billingAddress: ThreeDSecurePostalAddress? = null,
    var accountType: ThreeDSecureAccountType? = null,
    var additionalInformation: ThreeDSecureAdditionalInformation? = null,
    var challengeRequested: Boolean = false,
    var dataOnlyRequested: Boolean = false,
    var exemptionRequested: Boolean = false,
    var requestedExemptionType: ThreeDSecureRequestedExemptionType? = null,
    var cardAddChallengeRequested: Boolean? = null,
    var v2UiCustomization: ThreeDSecureV2UiCustomization? = null,
    var uiType: ThreeDSecureUiType = ThreeDSecureUiType.BOTH,
    var renderTypes: List<ThreeDSecureRenderType>? = null,
    var customFields: Map<String, String>? = null
) : Parcelable {

    /**
     * @return String representation of [ThreeDSecureRequest] for API use.
     */
    fun build(dfReferenceId: String?): String {
        val additionalInfo: JSONObject = additionalInformation?.toJson() ?: JSONObject()
        val base = JSONObject()
        try {
            base.put("amount", amount)
            base.put("additional_info", additionalInfo)
            accountType?.let { base.put("account_type", it.stringValue) }
            cardAddChallengeRequested?.let { base.put("card_add", it) }
            customFields?.let {
                if (it.isNotEmpty()) {
                    base.put("custom_fields", JSONObject(it))
                }
            }
            additionalInfo.putOpt("mobile_phone_number", mobilePhoneNumber)
            additionalInfo.putOpt("shipping_method", getShippingMethodAsString())
            additionalInfo.putOpt("email", email)
            billingAddress?.let {
                additionalInfo.putOpt("billing_given_name", it.givenName)
                additionalInfo.putOpt("billing_surname", it.surname)
                additionalInfo.putOpt("billing_line1", it.streetAddress)
                additionalInfo.putOpt("billing_line2", it.extendedAddress)
                additionalInfo.putOpt("billing_line3", it.line3)
                additionalInfo.putOpt("billing_city", it.locality)
                additionalInfo.putOpt("billing_state", it.region)
                additionalInfo.putOpt("billing_postal_code", it.postalCode)
                additionalInfo.putOpt("billing_country_code", it.countryCodeAlpha2)
                additionalInfo.putOpt("billing_phone_number", it.phoneNumber)
            }
            base.putOpt("df_reference_id", dfReferenceId)
            base.put("challenge_requested", challengeRequested)
            base.put("data_only_requested", dataOnlyRequested)
            base.put("exemption_requested", exemptionRequested)
            requestedExemptionType?.let {
                base.put("requested_exemption_type", it.stringValue)
            }
        } catch (ignored: JSONException) {
        }
        return base.toString()
    }

    private fun getShippingMethodAsString(): String? {
        return when (shippingMethod) {
            ThreeDSecureShippingMethod.SAME_DAY -> "01"
            ThreeDSecureShippingMethod.EXPEDITED -> "02"
            ThreeDSecureShippingMethod.PRIORITY -> "03"
            ThreeDSecureShippingMethod.GROUND -> "04"
            ThreeDSecureShippingMethod.ELECTRONIC_DELIVERY -> "05"
            ThreeDSecureShippingMethod.SHIP_TO_STORE -> "06"
            else -> null
        }
    }
}
