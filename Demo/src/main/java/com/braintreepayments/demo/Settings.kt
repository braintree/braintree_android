package com.braintreepayments.demo

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit

object Settings {

    private const val ENVIRONMENT = "environment"

    const val SANDBOX_ENV_NAME = "Sandbox"
    private const val PRODUCTION_ENV_NAME = "Production"

    private const val PRODUCTION_BASE_SERVER_URL = "https://braintree-production-merchant-455d21469113.herokuapp.com"
    private const val PRODUCTION_TOKENIZATION_KEY = "production_t2wns2y2_dfy45jdj3dxkmz5m"

    private const val SANDBOX_BASE_SERVER_URL = "https://braintree-demo-merchant-63b7a2204f6e.herokuapp.com"
    private const val SANDBOX_TOKENIZATION_KEY = "sandbox_tmxhyf7d_dcpspy2brwdjr3qn"

    private const val LOCAL_PAYMENTS_TOKENIZATION_KEY = "sandbox_f252zhq7_hh4cpc39zq4rgjcg"
    private const val XO_SANDBOX_TOKENIZATION_KEY = "sandbox_rz48bqvw_jcyycfw6f9j4nj9c"

    private var sharedPreferences: SharedPreferences? = null

    @JvmStatic
    fun getPreferences(context: Context): SharedPreferences {
        return sharedPreferences ?: run {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            sharedPreferences = prefs
            prefs
        }
    }

    @JvmStatic
    fun getEnvironment(context: Context): String {
        return getPreferences(context).getString(ENVIRONMENT, SANDBOX_ENV_NAME) ?: SANDBOX_ENV_NAME
    }

    @JvmStatic
    fun setEnvironment(context: Context, environment: String) {
        getPreferences(context).edit {
            putString(ENVIRONMENT, environment)
        }

        DemoApplication.resetApiClient()
    }

    @JvmStatic
    fun getSandboxUrl(): String {
        return SANDBOX_BASE_SERVER_URL
    }

    @JvmStatic
    fun getEnvironmentUrl(context: Context): String {
        val environment = getEnvironment(context)
        return when (environment) {
            SANDBOX_ENV_NAME -> SANDBOX_BASE_SERVER_URL
            PRODUCTION_ENV_NAME -> PRODUCTION_BASE_SERVER_URL
            else -> ""
        }
    }

    @JvmStatic
    fun getAuthorizationType(context: Context): String {
        return getPreferences(context).getString("authorization_type", context.getString(R.string.client_token))
            ?: context.getString(R.string.client_token)
    }

    @JvmStatic
    fun getCustomerId(context: Context): String? {
        return getPreferences(context).getString("customer", null)
    }

    @JvmStatic
    fun getMerchantAccountId(context: Context): String? {
        return getPreferences(context).getString("merchant_account", null)
    }

    @JvmStatic
    fun shouldCollectDeviceData(context: Context): Boolean {
        return getPreferences(context).getBoolean("collect_device_data", false)
    }

    @JvmStatic
    fun getThreeDSecureMerchantAccountId(context: Context): String? {
        return if (isThreeDSecureEnabled(context) && "production" == getEnvironment(context)) {
            "test_AIB"
        } else {
            null
        }
    }

    @JvmStatic
    fun getTokenizationKey(context: Context): String? {
        val environment = getEnvironment(context)
        return when (environment) {
            SANDBOX_ENV_NAME -> SANDBOX_TOKENIZATION_KEY
            PRODUCTION_ENV_NAME -> PRODUCTION_TOKENIZATION_KEY
            else -> null
        }
    }

    @JvmStatic
    fun getPayPalCheckoutTokenizationKey(context: Context): String? {
        val environment = getEnvironment(context)
        return when (environment) {
            SANDBOX_ENV_NAME -> XO_SANDBOX_TOKENIZATION_KEY
            PRODUCTION_ENV_NAME -> PRODUCTION_TOKENIZATION_KEY
            else -> null
        }
    }

    @JvmStatic
    fun getLocalPaymentsTokenizationKey(context: Context): String? {
        val environment = getEnvironment(context)
        return if (SANDBOX_ENV_NAME == environment) {
            LOCAL_PAYMENTS_TOKENIZATION_KEY
        } else {
            null
        }
    }

    @JvmStatic
    fun areGooglePayPrepaidCardsAllowed(context: Context): Boolean {
        return getPreferences(context).getBoolean("google_pay_allow_prepaid_cards", true)
    }

    @JvmStatic
    fun isGooglePayShippingAddressRequired(context: Context): Boolean {
        return getPreferences(context).getBoolean("google_pay_require_shipping_address", false)
    }

    @JvmStatic
    fun isGooglePayBillingAddressRequired(context: Context): Boolean {
        return getPreferences(context).getBoolean("google_pay_require_billing_address", false)
    }

    @JvmStatic
    fun isGooglePayPhoneNumberRequired(context: Context): Boolean {
        return getPreferences(context).getBoolean("google_pay_require_phone_number", false)
    }

    @JvmStatic
    fun isGooglePayEmailRequired(context: Context): Boolean {
        return getPreferences(context).getBoolean("google_pay_require_email", false)
    }

    @JvmStatic
    fun getGooglePayCurrency(context: Context): String {
        return getPreferences(context).getString("google_pay_currency", "EUR") ?: "EUR"
    }

    @JvmStatic
    fun getGooglePayAllowedCountriesForShipping(context: Context): List<String> {
        val preference = getPreferences(context).getString("google_pay_allowed_countries_for_shipping", "US") ?: "US"
        return preference.split(",").map { it.trim() }
    }

    @JvmStatic
    fun getPayPalIntentType(context: Context): String {
        return getPreferences(context).getString(
            "paypal_intent_type",
            context.getString(R.string.paypal_intent_authorize)
        )
            ?: context.getString(R.string.paypal_intent_authorize)
    }

    @JvmStatic
    fun getPayPalDisplayName(context: Context): String? {
        return getPreferences(context).getString("paypal_display_name", null)
    }

    @JvmStatic
    fun getPayPalLandingPageType(context: Context): String {
        return getPreferences(context).getString("paypal_landing_page_type", context.getString(R.string.none))
            ?: context.getString(R.string.none)
    }

    @JvmStatic
    fun isPayPalUseractionCommitEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean("paypal_useraction_commit", false)
    }

    @JvmStatic
    fun isPayPalCreditOffered(context: Context): Boolean {
        return getPreferences(context).getBoolean("paypal_credit_offered", false)
    }

    @JvmStatic
    fun isPayPalAppSwithEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean("paypal_app_switch", false)
    }

    @JvmStatic
    fun usePayPalAddressOverride(context: Context): Boolean {
        return getPreferences(context).getBoolean("paypal_address_override", true)
    }

    @JvmStatic
    fun isRbaMetadataEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean("paypal_rba_metadata", false)
    }

    @JvmStatic
    fun isThreeDSecureEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean("enable_three_d_secure", false)
    }

    @JvmStatic
    fun isThreeDSecureRequired(context: Context): Boolean {
        return getPreferences(context).getBoolean("require_three_d_secure", true)
    }

    @JvmStatic
    fun vaultVenmo(context: Context): Boolean {
        return getPreferences(context).getBoolean("vault_venmo", true)
    }

    @JvmStatic
    fun useAppLinkReturn(context: Context): Boolean {
        return getPreferences(context).getBoolean("use_app_link_return", true)
    }

    @JvmStatic
    fun isAmexRewardsBalanceEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean("amex_rewards_balance", false)
    }

    @JvmStatic
    fun showCheckoutExperience(context: Context): Boolean {
        return getPreferences(context).getBoolean("show_checkout_experience", false)
    }
}