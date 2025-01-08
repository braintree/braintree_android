package com.braintreepayments.api.paypal

import android.text.TextUtils
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.ClientToken
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.core.PostalAddressParser
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents the parameters that are needed to start the PayPal Checkout flow
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
 * @property intent Payment intent. Must be set to [PayPalPaymentIntent.SALE] for immediate payment,
 * [PayPalPaymentIntent.AUTHORIZE] to authorize a payment for capture later, or
 * [PayPalPaymentIntent.ORDER] to create an order.
 *
 * Defaults to authorize.
 *
 * @property amount The transaction amount in currency units (as * determined by setCurrencyCode).
 * For example, "1.20" corresponds to one dollar and twenty cents. Amount must be a non-negative
 * number, may optionally contain exactly 2 decimal places separated by '.' and is
 * limited to 7 digits before the decimal point.
 *
 * @property userAction The call-to-action in the PayPal Checkout flow.
 *
 * By default the final button will show the localized word for "Continue" and implies that the
 * final amount billed is not yet known. Setting the PayPalCheckoutRequest's userAction to
 * [PayPalPaymentUserAction.USER_ACTION_COMMIT] changes the button text to "Pay Now",
 * conveying to the user that billing will take place immediately.
 *
 * @property currencyCode A valid ISO currency code to use for the transaction. Defaults to merchant
 * currency code if not set.
 *
 * If unspecified, the currency code will be chosen based on the active merchant account in the
 * client token.
 *
 * @property shouldRequestBillingAgreement Whether to request billing agreement during checkout.
 *
 * If set to true, this enables the Checkout with Vault flow, where the customer will be prompted to
 * consent to a billing agreement during checkout.
 *
 * @property shouldOfferPayLater Offers PayPal Pay Later if the customer qualifies. Defaults to
 * false.
 *
 * @property contactInformation Contact information of the recipient for the order
 */
@Parcelize
class PayPalCheckoutRequest @JvmOverloads constructor(
    val amount: String,
    override val hasUserLocationConsent: Boolean,
    var intent: PayPalPaymentIntent = PayPalPaymentIntent.AUTHORIZE,
    var userAction: PayPalPaymentUserAction = PayPalPaymentUserAction.USER_ACTION_DEFAULT,
    var currencyCode: String? = null,
    var shouldRequestBillingAgreement: Boolean = false,
    var shouldOfferPayLater: Boolean = false,
    var contactInformation: PayPalContactInformation? = null,
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
    override var userPhoneNumber: PayPalPhoneNumber? = null,
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
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun createRequestBody(
        configuration: Configuration?,
        authorization: Authorization?,
        successUrl: String?,
        cancelUrl: String?,
        appLink: String?
    ): String {
        val parameters = JSONObject()
            .put(RETURN_URL_KEY, successUrl)
            .put(CANCEL_URL_KEY, cancelUrl)
            .put(OFFER_PAY_LATER_KEY, shouldOfferPayLater)

        if (authorization is ClientToken) {
            parameters.put(AUTHORIZATION_FINGERPRINT_KEY, authorization.bearer)
        } else {
            parameters.put(TOKENIZATION_KEY, authorization?.bearer)
        }

        if (shouldRequestBillingAgreement) {
            parameters.put(REQUEST_BILLING_AGREEMENT_KEY, true)
        }

        if (shouldRequestBillingAgreement && !TextUtils.isEmpty(billingAgreementDescription)) {
            val details = JSONObject().put(DESCRIPTION_KEY, billingAgreementDescription)
            parameters.put(BILLING_AGREEMENT_DETAILS_KEY, details)
        }

        userAuthenticationEmail?.let {
            if (it.isNotEmpty()) parameters.put(PAYER_EMAIL_KEY, it)
        }

        userPhoneNumber?.let { parameters.put(PHONE_NUMBER_KEY, it.toJson()) }

        contactInformation?.let { info ->
            info.recipientEmail?.let { parameters.put(RECIPIENT_EMAIL_KEY, it) }
            info.recipentPhoneNumber?.let { parameters.put(RECIPIENT_PHONE_NUMBER_KEY, it.toJson()) }
        }

        if (currencyCode == null) {
            currencyCode = configuration?.payPalCurrencyIsoCode
        }

        parameters
            .put(AMOUNT_KEY, amount)
            .put(CURRENCY_ISO_CODE_KEY, currencyCode)
            .put(INTENT_KEY, intent.stringValue)

        if (lineItems.isNotEmpty()) {
            val jsonLineItems = JSONArray()
            lineItems.forEach { jsonLineItems.put(it.toJson()) }
            parameters.put(LINE_ITEMS_KEY, jsonLineItems)
        }

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

        if (userAction != PayPalPaymentUserAction.USER_ACTION_DEFAULT) {
            experienceProfile.put(USER_ACTION_KEY, userAction.stringValue)
        }

        shippingAddressOverride?.let {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, !isShippingAddressEditable)

            parameters.put(PostalAddressParser.LINE_1_KEY, it.streetAddress)
            parameters.put(PostalAddressParser.LINE_2_KEY, it.extendedAddress)
            parameters.put(PostalAddressParser.LOCALITY_KEY, it.locality)
            parameters.put(PostalAddressParser.REGION_KEY, it.region)
            parameters.put(
                PostalAddressParser.POSTAL_CODE_UNDERSCORE_KEY,
                it.postalCode
            )
            parameters.put(
                PostalAddressParser.COUNTRY_CODE_UNDERSCORE_KEY,
                it.countryCodeAlpha2
            )
            parameters.put(
                PostalAddressParser.RECIPIENT_NAME_UNDERSCORE_KEY,
                it.recipientName
            )
        } ?: run {
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
