package com.braintreepayments.api.googlepay

import android.os.Parcelable
import android.text.TextUtils
import com.google.android.gms.wallet.ShippingAddressRequirements
import com.google.android.gms.wallet.TransactionInfo
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.WalletConstants.BillingAddressFormat
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

/**
 * Represents the parameters that are needed to use the Google Pay API.
 *
 * Details and the price of the transaction. Required.
 * @property transactionInfo See [TransactionInfo].
 *
 * Optional.
 * @property emailRequired `true` if the buyer's email address is required to be returned, `false` otherwise.
 *
 * Optional.
 * @property phoneNumberRequired `true` if the buyer's phone number is required to be returned as part of the
 * billing address and shipping address, `false` otherwise.
 *
 * Optional.
 * @property billingAddressRequired `true` if the buyer's billing address is required to be returned,
 * false` otherwise.
 *
 * Optional.
 * @property billingAddressFormat the billing address format to return. [BillingAddressFormat]
 *
 * Optional.
 * @property shippingAddressRequired `true` if the buyer's shipping address is required to be returned,
 * `false` otherwise.
 *
 * Optional.
 * @property shippingAddressRequirements the shipping address requirements. [ShippingAddressRequirements]
 *
 * Optional.
 * @property allowPrepaidCards `true` prepaid cards are allowed, `false` otherwise.
 *
 * Defines if PayPal should be an available payment method in Google Pay.
 * Defaults to `true`.
 * @property enablePayPal `true` by default. Allows PayPal to be a payment method in Google Pay.
 *
 * Optional.
 * @property merchantName The merchant name that will be presented in Google Pay
 *
 * ISO 3166-1 alpha-2 country code where the transaction is processed. This is required for
 * merchants based in European Economic Area (EEA) countries.
 * NOTE: to support Elo cards, country code must be set to "BR"
 * @property countryCode The country code where the transaction is processed
 *
 * Optional
 * @property totalPriceLabel Custom label for the total price within the display items
 */
@Suppress("TooManyFunctions")
@Parcelize
class GooglePayRequest(
    // NEXT_MAJOR_VERSION: allow merchants to set transaction info params individually and build
    // JSON object under the hood
    var transactionInfo: TransactionInfo? = null,
    var isEmailRequired: Boolean = false,
    var isPhoneNumberRequired: Boolean = false,
    var isBillingAddressRequired: Boolean = false,
    @BillingAddressFormat
    var billingAddressFormat: Int = 0,
    var isShippingAddressRequired: Boolean = false,
    // NEXT_MAJOR_VERSION: allow merchants to set shipping address requirements params individually
    // and build JSON object under the hood
    var shippingAddressRequirements: ShippingAddressRequirements? = null,
    var allowPrepaidCards: Boolean = false,
    var isPayPalEnabled: Boolean = true,
    var googleMerchantName: String? = null,
    var countryCode: String? = null,
    var totalPriceLabel: String? = null,
    var allowCreditCards: Boolean = true,
    private var environment: String? = null,
    private val allowedPaymentMethods: MutableMap<String, String> = HashMap(),
    private val tokenizationSpecifications: MutableMap<String, String> = HashMap(),
    private val allowedAuthMethods: MutableMap<String, String> = HashMap(),
    private val allowedCardNetworks: MutableMap<String, String> = HashMap()
) : Parcelable {

    fun setEnvironment(environment: String?) {
        this.environment =
            if ("PRODUCTION" == environment?.uppercase(Locale.getDefault())) "PRODUCTION" else "TEST"
    }

    fun getEnvironment(): String? {
        return environment
    }

    /**
     * Simple wrapper to assign given parameters to specified paymentMethod
     *
     * @param paymentMethodType The paymentMethod to add to
     * @param parameters        Parameters to assign to the paymentMethod
     */
    fun setAllowedPaymentMethod(paymentMethodType: String, parameters: JSONObject) {
        allowedPaymentMethods[paymentMethodType] = parameters.toString()
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's tokenizationSpecification
     *
     * @param paymentMethodType The paymentMethod to attached tokenizationSpecification parameters to
     * @param parameters        The tokenizationSpecification parameters to attach
     */
    fun setTokenizationSpecificationForType(paymentMethodType: String, parameters: JSONObject) {
        tokenizationSpecifications[paymentMethodType] = parameters.toString()
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's allowedAuthMethods
     *
     * @param paymentMethodType the paymentMethod to attach allowedAuthMethods to
     * @param authMethods       the authMethods to allow the paymentMethodType to transact with
     */
    fun setAllowedAuthMethods(paymentMethodType: String, authMethods: JSONArray) {
        allowedAuthMethods[paymentMethodType] = authMethods.toString()
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's cardNetworks
     *
     * @param paymentMethodType the paymentMethod to attach cardNetworks to
     * @param cardNetworks      the cardNetworks to allow the paymentMethodType to transact with
     */
    fun setAllowedCardNetworks(paymentMethodType: String, cardNetworks: JSONArray) {
        allowedCardNetworks[paymentMethodType] = cardNetworks.toString()
    }

    /**
     * Assemble all declared parts of a GooglePayRequest to a JSON string
     * for use in making requests against Google
     *
     * @return String
     */

    @SuppressWarnings("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth")
    fun toJson(): String {
        val transactionInfoJson = JSONObject()
        val transactionInfo = transactionInfo
        val allowedPaymentMethods = JSONArray()
        val shippingAddressParameters = JSONObject()
        val allowedCountryCodeList: ArrayList<String?>?

        if (isShippingAddressRequired) {
            allowedCountryCodeList = shippingAddressRequirements?.allowedCountryCodes

            if (allowedCountryCodeList != null && allowedCountryCodeList.size > 0) {
                try {
                    shippingAddressParameters.put(
                        "allowedCountryCodes",
                        JSONArray(allowedCountryCodeList)
                    )
                } catch (ignored: JSONException) {
                }
            }
            try {
                shippingAddressParameters.put("phoneNumberRequired", isPhoneNumberRequired)
            } catch (ignored: JSONException) {
            }
        }

        try {
            val totalPriceStatus = totalPriceStatusToString()
            transactionInfoJson
                .put("totalPriceStatus", totalPriceStatus)

            if (transactionInfo != null) {
                transactionInfoJson.put("totalPrice", transactionInfo.totalPrice)
                transactionInfoJson.put("currencyCode", transactionInfo.currencyCode)
            }

            countryCode?.let {
                transactionInfoJson.put("countryCode", it)
            }

            totalPriceLabel?.let {
                transactionInfoJson.put("totalPriceLabel", it)
            }
        } catch (ignored: JSONException) {
        }

        for ((key, value) in this.allowedPaymentMethods) {
            try {
                val paymentMethod = JSONObject()
                    .put("type", key)
                    .put("parameters", JSONObject(value))
                val tokenSpec = tokenizationSpecifications[key]
                if (tokenSpec != null) {
                    paymentMethod.put("tokenizationSpecification", JSONObject(tokenSpec))
                }

                if ("CARD" == key) {
                    val paymentMethodParams = paymentMethod.getJSONObject("parameters")
                    paymentMethodParams
                        .put("billingAddressRequired", isBillingAddressRequired)
                        .put("allowPrepaidCards", allowPrepaidCards)
                        .put("allowCreditCards", allowCreditCards)
                    try {
                        val billingAddressParameters =
                            JSONObject(value)["billingAddressParameters"]
                        paymentMethodParams
                            .put("billingAddressParameters", billingAddressParameters)
                    } catch (ignored: JSONException) {
                        if (isBillingAddressRequired) {
                            paymentMethodParams
                                .put(
                                    "billingAddressParameters", JSONObject()
                                        .put("format", billingAddressFormatToString())
                                        .put("phoneNumberRequired", isPhoneNumberRequired)
                                )
                        }
                    }
                }

                allowedPaymentMethods.put(paymentMethod)
            } catch (ignored: JSONException) {
            }
        }

        val merchantInfo = JSONObject()

        try {
            if (!TextUtils.isEmpty(googleMerchantName)) {
                merchantInfo.put("merchantName", googleMerchantName)
            }
        } catch (ignored: JSONException) {
        }

        val json = JSONObject()

        try {
            json
                .put("apiVersion", 2)
                .put("apiVersionMinor", 0)
                .put("allowedPaymentMethods", allowedPaymentMethods)
                .put("emailRequired", isEmailRequired)
                .put("shippingAddressRequired", isShippingAddressRequired)
                .put("environment", environment)
                .put("merchantInfo", merchantInfo)
                .put("transactionInfo", transactionInfoJson)

            if (isShippingAddressRequired) {
                json.put("shippingAddressParameters", shippingAddressParameters)
            }
        } catch (ignored: JSONException) {
        }

        return json.toString()
    }

    private fun totalPriceStatusToString(): String {

        return transactionInfo?.let {
            when (it.totalPriceStatus) {
                WalletConstants.TOTAL_PRICE_STATUS_NOT_CURRENTLY_KNOWN -> "NOT_CURRENTLY_KNOWN"
                WalletConstants.TOTAL_PRICE_STATUS_ESTIMATED -> "ESTIMATED"
                WalletConstants.TOTAL_PRICE_STATUS_FINAL -> "FINAL"
                else -> "FINAL"
            }
        } ?: run {
            "FINAL"
        }
    }

    fun billingAddressFormatToString(): String {
        var format = "MIN"
        if (billingAddressFormat == WalletConstants.BILLING_ADDRESS_FORMAT_FULL) {
            format = "FULL"
        }
        return format
    }

    /**
     * @return Allowed payment methods for a given payment method type.
     */
    fun getAllowedPaymentMethod(type: String): JSONObject? {
        return allowedPaymentMethods[type]?.let {
            JSONObject(it)
        }
    }

    /**
     * @return Tokenization specification for a given payment method type.
     */
    fun getTokenizationSpecificationForType(type: String): JSONObject? {
        return tokenizationSpecifications[type]?.let {
            JSONObject(it)
        }
    }

    /**
     * @return Allowed authentication methods for a given payment method type.
     */
    fun getAllowedAuthMethodsForType(type: String): JSONArray? {
        return allowedAuthMethods[type]?.let {
            JSONArray(it)
        }
    }

    /**
     * @return Allowed card networks for a given payment method type.
     */
    fun getAllowedCardNetworksForType(type: String): JSONArray? {
        return allowedCardNetworks[type]?.let {
            JSONArray(it)
        }
    }
}
