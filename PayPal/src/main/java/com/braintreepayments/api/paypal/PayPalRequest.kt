package com.braintreepayments.api.paypal

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.PostalAddress
import org.json.JSONException

/**
 * Represents the parameters that are needed to tokenize a PayPal account. See
 * [PayPalCheckoutRequest] and [PayPalVaultRequest].
 *
 * @property hasUserLocationConsent is a required parameter that informs the SDK if your application
 * has obtained consent from the user to collect location data in compliance with
 * [Google Play Developer Program policies](https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive)
 * This flag enables PayPal to collect necessary information required for Fraud Detection and Risk
 * Management.
 * @see [User Data policies for the Google Play Developer Program ](https://support.google.com/googleplay/android-developer/answer/10144311.personal-sensitive)
 *
 * @see [Examples of prominent in-app disclosures](https://support.google.com/googleplay/android-developer/answer/9799150?hl=en.Prominent%20in-app%20disclosure)
 *
 * @property localeCode A locale code to use for the transaction.
 * Supported locales are:
 * <br></br>
 * `da_DK`,
 * `de_DE`,
 * `en_AU`,
 * `en_GB`,
 * `en_US`,
 * `es_ES`,
 * `es_XC`,
 * `fr_CA`,
 * `fr_FR`,
 * `fr_XC`,
 * `id_ID`,
 * `it_IT`,
 * `ja_JP`,
 * `ko_KR`,
 * `nl_NL`,
 * `no_NO`,
 * `pl_PL`,
 * `pt_BR`,
 * `pt_PT`,
 * `ru_RU`,
 * `sv_SE`,
 * `th_TH`,
 * `tr_TR`,
 * `zh_CN`,
 * `zh_HK`,
 * `zh_TW`,
 * `zh_XC`.
 *
 * @property billingAgreementDescription Display a custom description to the user for a billing
 * agreement. This property is optional.
 * @property isShippingAddressRequired Whether to hide the shipping address in the flow. Defaults to
 * false. When set to true, the shipping address selector will be displayed.
 * @property isShippingAddressEditable Whether to allow the the shipping address to be editable.
 * Defaults to false. Set to true to enable user editing of the shipping address. Only applies
 * when [PayPalRequest.shippingAddressOverride] is set with a [PostalAddress].
 * @property shippingAddressOverride a custom [PostalAddress], A valid shipping address to be
 * displayed in the transaction flow. An error will occur if this address is not valid.
 * @property landingPageType Use this option to specify the PayPal page to display when a user lands
 * on the PayPal site to complete the payment.
 * @property displayName The merchant name displayed in the PayPal flow; defaults to the company
 * name on your Braintree account.
 * @property merchantAccountId Specify a merchant account Id other than the default to use during
 * tokenization.
 * @property riskCorrelationId A risk correlation ID created with Set Transaction Context on your
 * server.
 * @property userAuthenticationEmail User email to initiate a quicker authentication flow in cases
 * where the user has a PayPal Account with the same email.
 * @property userPhoneNumber User phone number used to initiate a quicker authentication flow in
 * cases where the user has a PayPal Account with the phone number.
 * @property lineItems The line items for this transaction. It can include up to 249 line items.
 */
abstract class PayPalRequest internal constructor(
    open val hasUserLocationConsent: Boolean,
    open var localeCode: String? = null,
    open var billingAgreementDescription: String? = null,
    open var isShippingAddressRequired: Boolean = false,
    open var isShippingAddressEditable: Boolean = false,
    open var shippingAddressOverride: PostalAddress? = null,
    open var landingPageType: PayPalLandingPageType? = null,
    open var displayName: String? = null,
    open var merchantAccountId: String? = null,
    open var riskCorrelationId: String? = null,
    open var userAuthenticationEmail: String? = null,
    open var userPhoneNumber: PayPalPhoneNumber? = null,
    open var lineItems: List<PayPalLineItem> = emptyList()
) : Parcelable {

    @Throws(JSONException::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract fun createRequestBody(
        configuration: Configuration?,
        authorization: Authorization?,
        successUrl: String?,
        cancelUrl: String?,
        appLink: String?
    ): String?

    companion object {
        internal const val NO_SHIPPING_KEY: String = "no_shipping"
        internal const val ADDRESS_OVERRIDE_KEY: String = "address_override"
        internal const val LOCALE_CODE_KEY: String = "locale_code"
        internal const val REQUEST_BILLING_AGREEMENT_KEY: String = "request_billing_agreement"
        internal const val BILLING_AGREEMENT_DETAILS_KEY: String = "billing_agreement_details"
        internal const val DESCRIPTION_KEY: String = "description"
        internal const val PAYER_EMAIL_KEY: String = "payer_email"
        internal const val AUTHORIZATION_FINGERPRINT_KEY: String = "authorization_fingerprint"
        internal const val TOKENIZATION_KEY: String = "client_key"
        internal const val RETURN_URL_KEY: String = "return_url"
        internal const val OFFER_CREDIT_KEY: String = "offer_paypal_credit"
        internal const val OFFER_PAY_LATER_KEY: String = "offer_pay_later"
        internal const val CANCEL_URL_KEY: String = "cancel_url"
        internal const val EXPERIENCE_PROFILE_KEY: String = "experience_profile"
        internal const val AMOUNT_KEY: String = "amount"
        internal const val CURRENCY_ISO_CODE_KEY: String = "currency_iso_code"
        internal const val INTENT_KEY: String = "intent"
        internal const val LANDING_PAGE_TYPE_KEY: String = "landing_page_type"
        internal const val DISPLAY_NAME_KEY: String = "brand_name"
        internal const val SHIPPING_ADDRESS_KEY: String = "shipping_address"
        internal const val MERCHANT_ACCOUNT_ID: String = "merchant_account_id"
        internal const val CORRELATION_ID_KEY: String = "correlation_id"
        internal const val LINE_ITEMS_KEY: String = "line_items"
        internal const val USER_ACTION_KEY: String = "user_action"
        internal const val ENABLE_APP_SWITCH_KEY: String = "launch_paypal_app"
        internal const val OS_VERSION_KEY: String = "os_version"
        internal const val OS_TYPE_KEY: String = "os_type"
        internal const val MERCHANT_APP_RETURN_URL_KEY: String = "merchant_app_return_url"
        internal const val PHONE_NUMBER_KEY: String = "phone_number"
    }
}
