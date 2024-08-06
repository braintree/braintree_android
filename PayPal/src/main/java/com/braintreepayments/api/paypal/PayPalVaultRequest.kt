package com.braintreepayments.api.paypal

import android.text.TextUtils
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.ClientToken
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.core.PostalAddressParser
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents the parameters that are needed to start the PayPal Vault flow
 *
 * @property hasUserLocationConsent is a required property that informs the SDK if your application
 * has obtained consent from the user to collect location data in compliance with
 * [Google Play Developer Program policies](https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive)
 * This flag enables PayPal to collect necessary information required for Fraud Detection and Risk Management.
 *
 * @see [User Data policies for the Google Play Developer Program ](https://support.google.com/googleplay/android-developer/answer/10144311.personal-sensitive)
 *
 * @see [Examples of prominent in-app disclosures](https://support.google.com/googleplay/android-developer/answer/9799150?hl=en.Prominent%20in-app%20disclosure)
 *
 * @property shouldOfferCredit Offers PayPal Credit if the customer qualifies. Defaults to false.
 */
@Parcelize
class PayPalVaultRequest
@JvmOverloads constructor(
    override val hasUserLocationConsent: Boolean,
    var shouldOfferCredit: Boolean = false,
    override var localeCode: String? = null,
    override var billingAgreementDescription: String? = null,
    override var isShippingAddressRequired: Boolean = false,
    override var isShippingAddressEditable: Boolean = false,
    override var shippingAddressOverride: PostalAddress? = null,
    override var landingPageType: PayPalLandingPageType? = null,
    override var displayName: String? = null,
    override var merchantAccountId: String? = null,
    override var riskCorrelationId: String? = null,
    override var userAuthenticationEmail: String? = null,
    override var lineItems: List<PayPalLineItem> = emptyList(),
) : PayPalRequest(
    hasUserLocationConsent = hasUserLocationConsent,
    localeCode = localeCode,
    billingAgreementDescription = billingAgreementDescription,
    isShippingAddressRequired = isShippingAddressRequired,
    isShippingAddressEditable = isShippingAddressEditable,
    shippingAddressOverride = shippingAddressOverride,
    landingPageType = landingPageType,
    displayName = displayName,
    merchantAccountId = merchantAccountId,
    riskCorrelationId = riskCorrelationId,
    userAuthenticationEmail = userAuthenticationEmail,
    lineItems = lineItems
) {

    @Throws(JSONException::class)
    @Suppress("LongMethod")
    override fun createRequestBody(
        configuration: Configuration?,
        authorization: Authorization?,
        successUrl: String?,
        cancelUrl: String?
    ): String {
        val parameters = JSONObject()
            .put(RETURN_URL_KEY, successUrl)
            .put(CANCEL_URL_KEY, cancelUrl)
            .put(OFFER_CREDIT_KEY, shouldOfferCredit)

        if (authorization is ClientToken) {
            parameters.put(AUTHORIZATION_FINGERPRINT_KEY, authorization.bearer)
        } else {
            parameters.put(TOKENIZATION_KEY, authorization?.bearer)
        }

        val billingAgreementDescription = billingAgreementDescription
        if (!TextUtils.isEmpty(billingAgreementDescription)) {
            parameters.put(DESCRIPTION_KEY, billingAgreementDescription)
        }

        parameters.putOpt(PAYER_EMAIL_KEY, userAuthenticationEmail)

        val experienceProfile = JSONObject()
        experienceProfile.put(NO_SHIPPING_KEY, !isShippingAddressRequired)
        experienceProfile.put(LANDING_PAGE_TYPE_KEY, landingPageType?.stringValue)
        var displayName = displayName
        if (TextUtils.isEmpty(displayName)) {
            displayName = configuration?.payPalDisplayName
        }
        experienceProfile.put(DISPLAY_NAME_KEY, displayName)

        if (localeCode != null) {
            experienceProfile.put(LOCALE_CODE_KEY, localeCode)
        }

        if (shippingAddressOverride != null) {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, !isShippingAddressEditable)

            val shippingAddressJson = JSONObject()
            parameters.put(SHIPPING_ADDRESS_KEY, shippingAddressJson)

            val shippingAddress = shippingAddressOverride
            shippingAddressJson.put(
                PostalAddressParser.LINE_1_KEY,
                shippingAddress?.streetAddress
            )
            shippingAddressJson.put(
                PostalAddressParser.LINE_2_KEY,
                shippingAddress?.extendedAddress
            )
            shippingAddressJson.put(
                PostalAddressParser.LOCALITY_KEY,
                shippingAddress?.locality
            )
            shippingAddressJson.put(PostalAddressParser.REGION_KEY, shippingAddress?.region)
            shippingAddressJson.put(
                PostalAddressParser.POSTAL_CODE_UNDERSCORE_KEY,
                shippingAddress?.postalCode
            )
            shippingAddressJson.put(
                PostalAddressParser.COUNTRY_CODE_UNDERSCORE_KEY,
                shippingAddress?.countryCodeAlpha2
            )
            shippingAddressJson.put(
                PostalAddressParser.RECIPIENT_NAME_UNDERSCORE_KEY,
                shippingAddress?.recipientName
            )
        } else {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, false)
        }

        if (merchantAccountId != null) {
            parameters.put(MERCHANT_ACCOUNT_ID, merchantAccountId)
        }

        if (riskCorrelationId != null) {
            parameters.put(CORRELATION_ID_KEY, riskCorrelationId)
        }

        parameters.put(EXPERIENCE_PROFILE_KEY, experienceProfile)
        return parameters.toString()
    }
}
