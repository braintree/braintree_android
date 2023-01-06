package com.braintreepayments.api

import androidx.annotation.RestrictTo
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Contains the remote configuration for the Braintree Android SDK.
 */
class Configuration internal constructor(configurationString: String) {

    companion object {
        private const val ASSETS_URL_KEY = "assetsUrl"
        private const val CLIENT_API_URL_KEY = "clientApiUrl"
        private const val CHALLENGES_KEY = "challenges"
        private const val ENVIRONMENT_KEY = "environment"
        private const val MERCHANT_ID_KEY = "merchantId"
        private const val MERCHANT_ACCOUNT_ID_KEY = "merchantAccountId"
        private const val ANALYTICS_KEY = "analytics"
        private const val BRAINTREE_API_KEY = "braintreeApi"
        private const val PAYPAL_ENABLED_KEY = "paypalEnabled"
        private const val PAYPAL_KEY = "paypal"
        private const val KOUNT_KEY = "kount"
        private const val GOOGLE_PAY_KEY = "androidPay"
        private const val THREE_D_SECURE_ENABLED_KEY = "threeDSecureEnabled"
        private const val PAY_WITH_VENMO_KEY = "payWithVenmo"
        private const val UNIONPAY_KEY = "unionPay"
        private const val CARD_KEY = "creditCards"
        private const val VISA_CHECKOUT_KEY = "visaCheckout"
        private const val GRAPHQL_KEY = "graphQL"
        private const val SAMSUNG_PAY_KEY = "samsungPay"
        private const val CARDINAL_AUTHENTICATION_JWT = "cardinalAuthenticationJWT"

        /**
         * Creates a new [Configuration] instance from a json string.
         *
         * @param configurationString The json configuration string from Braintree.
         * @return [Configuration] instance.
         */
        @Throws(JSONException::class)
        @JvmStatic
        fun fromJson(configurationString: String): Configuration {
            return Configuration(configurationString)
        }
    }

    private val configurationString: String
    private val challenges: MutableSet<String> = HashSet()

    private val braintreeApiConfiguration: BraintreeApiConfiguration
    private val analyticsConfiguration: AnalyticsConfiguration
    private val cardConfiguration: CardConfiguration

    private val payPalConfiguration: PayPalConfiguration
    private val googlePayConfiguration: GooglePayConfiguration

    private val venmoConfiguration: VenmoConfiguration
    private val kountConfiguration: KountConfiguration
    private val unionPayConfiguration: UnionPayConfiguration
    private val visaCheckoutConfiguration: VisaCheckoutConfiguration
    private val graphQLConfiguration: GraphQLConfiguration
    private val samsungPayConfiguration: SamsungPayConfiguration
    private val cardinalAuthenticationJwt: String?

    /**
     * @return The assets URL of the current environment.
     */
    val assetsUrl: String

    /**
     * @return The url of the Braintree client API for the current environment.
     */
    val clientApiUrl: String

    /**
     * @return The current environment.
     */
    val environment: String

    /**
     * @return the current Braintree merchant id.
     */
    val merchantId: String

    /**
     * @return `true` if PayPal is enabled and supported in the current environment,
     * `false` otherwise.
     */
    val isPayPalEnabled: Boolean

    /**
     * @return `true` if 3D Secure is enabled and supported for the current merchant account,
     * `false` otherwise.
     */
    val isThreeDSecureEnabled: Boolean

    /**
     * @return the current Braintree merchant account id.
     */
    val merchantAccountId: String?

    init {
        this.configurationString = configurationString
        val json = JSONObject(configurationString)
        assetsUrl = Json.optString(json, ASSETS_URL_KEY, "")
        clientApiUrl = json.getString(CLIENT_API_URL_KEY)
        parseJsonChallenges(json.optJSONArray(CHALLENGES_KEY))
        environment = json.getString(ENVIRONMENT_KEY)
        merchantId = json.getString(MERCHANT_ID_KEY)
        merchantAccountId = Json.optString(json, MERCHANT_ACCOUNT_ID_KEY, null)
        analyticsConfiguration = AnalyticsConfiguration.fromJson(json.optJSONObject(ANALYTICS_KEY))
        braintreeApiConfiguration = BraintreeApiConfiguration.fromJson(
            json.optJSONObject(
                BRAINTREE_API_KEY
            )
        )
        cardConfiguration = CardConfiguration.fromJson(json.optJSONObject(CARD_KEY))
        isPayPalEnabled = json.optBoolean(PAYPAL_ENABLED_KEY, false)
        payPalConfiguration = PayPalConfiguration.fromJson(json.optJSONObject(PAYPAL_KEY))
        googlePayConfiguration = GooglePayConfiguration.fromJson(json.optJSONObject(GOOGLE_PAY_KEY))
        isThreeDSecureEnabled = json.optBoolean(THREE_D_SECURE_ENABLED_KEY, false)
        venmoConfiguration = VenmoConfiguration.fromJson(json.optJSONObject(PAY_WITH_VENMO_KEY))
        kountConfiguration = KountConfiguration.fromJson(json.optJSONObject(KOUNT_KEY))
        unionPayConfiguration = UnionPayConfiguration.fromJson(json.optJSONObject(UNIONPAY_KEY))
        visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(
            json.optJSONObject(
                VISA_CHECKOUT_KEY
            )
        )
        graphQLConfiguration = GraphQLConfiguration.fromJson(json.optJSONObject(GRAPHQL_KEY))
        samsungPayConfiguration = SamsungPayConfiguration.fromJson(
            json.optJSONObject(
                SAMSUNG_PAY_KEY
            )
        )
        cardinalAuthenticationJwt = Json.optString(json, CARDINAL_AUTHENTICATION_JWT, null)
    }

    fun toJson(): String {
        return configurationString
    }

    /**
     * @return `true` if cvv is required for card transactions, `false` otherwise.
     */
    val isCvvChallengePresent = challenges.contains("cvv")

    /**
     * @return `true` if postal code is required for card transactions, `false` otherwise.
     */
    val isPostalCodeChallengePresent = challenges.contains("postal_code")

    /**
     * @return `true` if fraud device data collection should occur; `false` otherwise.
     */
    val isFraudDataCollectionEnabled = cardConfiguration.isFraudDataCollectionEnabled

    /**
     * @return `true` if Venmo is enabled for the merchant account; `false` otherwise.
     */
    val isVenmoEnabled = venmoConfiguration.isAccessTokenValid

    /**
     * @return the Access Token used by the Venmo app to tokenize on behalf of the merchant.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val venmoAccessToken = venmoConfiguration.accessToken

    /**
     * @return the Venmo merchant id used by the Venmo app to authorize payment.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val venmoMerchantId = venmoConfiguration.merchantId

    /**
     * @return the Venmo environment used to handle this payment.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val venmoEnvironment = venmoConfiguration.environment

    /**
     * @return `true` if GraphQL is enabled for the merchant account; `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val isGraphQLEnabled = graphQLConfiguration.isEnabled // Local Payments are enabled when PayPal is enabled

    /**
     * @return `true` if Local Payment is enabled for the merchant account; `false` otherwise.
     */
    val isLocalPaymentEnabled = isPayPalEnabled // Local Payments are enabled when PayPal is enabled

    /**
     * @return `true` if Kount is enabled for the merchant account; `false` otherwise.
     */
    val isKountEnabled = kountConfiguration.isEnabled

    /**
     * @return the Kount merchant id set in the Gateway.
     */
    val kountMerchantId = kountConfiguration.kountMerchantId

    /**
     * @return `true` if UnionPay is enabled for the merchant account; `false` otherwise.
     */
    val isUnionPayEnabled = unionPayConfiguration.isEnabled

    /**
     * @return the PayPal app display name.
     */
    val payPalDisplayName = payPalConfiguration.displayName

    /**
     * @return the PayPal app client id.
     */
    val payPalClientId = payPalConfiguration.clientId

    /**
     * @return the PayPal app privacy url.
     */
    val payPalPrivacyUrl = payPalConfiguration.privacyUrl

    /**
     * @return the PayPal app user agreement url.
     */
    val payPalUserAgreementUrl = payPalConfiguration.userAgreementUrl

    /**
     * @return the url for custom PayPal environments.
     */
    val payPalDirectBaseUrl = payPalConfiguration.directBaseUrl

    /**
     * @return the current environment for PayPal.
     */
    val payPalEnvironment = payPalConfiguration.environment

    /**
     * @return `true` if PayPal touch is currently disabled, `false` otherwise.
     */
    val isPayPalTouchDisabled = payPalConfiguration.isTouchDisabled

    /**
     * @return the PayPal currency code.
     */
    val payPalCurrencyIsoCode = payPalConfiguration.currencyIsoCode

    /**
     * @return `true` if Google Payment is enabled and supported in the current environment; `false` otherwise.
     */
    val isGooglePayEnabled = googlePayConfiguration.isEnabled

    /**
     * @return the authorization fingerprint to use for Google Payment, only allows tokenizing Google Payment cards.
     */
    val googlePayAuthorizationFingerprint = googlePayConfiguration.googleAuthorizationFingerprint

    /**
     * @return the current Google Pay environment.
     */
    val googlePayEnvironment = googlePayConfiguration.environment

    /**
     * @return the Google Pay display name to show to the user.
     */
    val googlePayDisplayName = googlePayConfiguration.displayName

    /**
     * @return a list of supported card networks for Google Pay.
     */
    val googlePaySupportedNetworks = googlePayConfiguration.supportedNetworks

    /**
     * @return the PayPal Client ID used by Google Pay.
     */
    val googlePayPayPalClientId = googlePayConfiguration.paypalClientId

    /**
     * @return [String] url of the Braintree analytics service.
     */
    val analyticsUrl = analyticsConfiguration.url

    /**
     * @return `true` if analytics are enabled, `false` otherwise.
     */
    val isAnalyticsEnabled = analyticsConfiguration.isEnabled

    /**
     * @return `true` if Visa Checkout is enabled for the merchant account; `false` otherwise.
     */
    val isVisaCheckoutEnabled = visaCheckoutConfiguration.isEnabled

    /**
     * @return the Visa Checkout supported networks enabled for the merchant account.
     */
    val visaCheckoutSupportedNetworks = visaCheckoutConfiguration.acceptedCardBrands

    /**
     * @return the Visa Checkout API key configured in the Braintree Control Panel.
     */
    val visaCheckoutApiKey = visaCheckoutConfiguration.apiKey

    /**
     * @return the Visa Checkout External Client ID configured in the Braintree Control Panel.
     */
    val visaCheckoutExternalClientId = visaCheckoutConfiguration.externalClientId

    /**
     * Check if a specific feature is enabled in the GraphQL API.
     *
     * @param feature The feature to check.
     * @return `true` if GraphQL is enabled and the feature is enabled, `false` otherwise.
     */
    fun isGraphQLFeatureEnabled(feature: String?): Boolean {
        return graphQLConfiguration.isFeatureEnabled(feature)
    }

    /**
     * @return the GraphQL url.
     */
    val graphQLUrl = graphQLConfiguration.url

    /**
     * @return `true` if Samsung Pay is enabled; `false` otherwise.
     */
    val isSamsungPayEnabled = samsungPayConfiguration.isEnabled

    /**
     * @return the merchant display name for Samsung Pay.
     */
    val samsungPayMerchantDisplayName = samsungPayConfiguration.merchantDisplayName

    /**
     * @return the Samsung Pay service id associated with the merchant.
     */
    val samsungPayServiceId = samsungPayConfiguration.serviceId

    /**
     * @return a list of card brands supported by Samsung Pay.
     */
    val samsungPaySupportedCardBrands = ArrayList(samsungPayConfiguration.supportedCardBrands)

    /**
     * @return the authorization to use with Samsung Pay.
     */
    val samsungPayAuthorization = samsungPayConfiguration.samsungAuthorization

    /**
     * @return the Braintree environment Samsung Pay should interact with.
     */
    val samsungPayEnvironment = samsungPayConfiguration.environment

    private fun parseJsonChallenges(jsonArray: JSONArray?) {
        if (jsonArray != null) {
            for (i in 0 until jsonArray.length()) {
                challenges.add(jsonArray.optString(i, ""))
            }
        }
    }

    /**
     * @return the JWT for Cardinal
     */
    fun getCardinalAuthenticationJwt(): String? {
        return cardinalAuthenticationJwt
    }

    /**
     * @return The Access Token for Braintree API.
     */
    val braintreeApiAccessToken = braintreeApiConfiguration.accessToken

    /**
     * @return the base url for accessing Braintree API.
     */
    val braintreeApiUrl = braintreeApiConfiguration.url

    /**
     * @return a boolean indicating whether Braintree API is enabled for this merchant.
     */
    val isBraintreeApiEnabled = braintreeApiConfiguration.isEnabled

    /**
     * @return a [<] of card types supported by the merchant.
     */
    val supportedCardTypes = cardConfiguration.supportedCardTypes

}