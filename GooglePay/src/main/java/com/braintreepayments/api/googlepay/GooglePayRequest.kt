package com.braintreepayments.api.googlepay

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

/**
 * Represents the parameters that are needed to use the Google Pay API.
 *
 * @property currencyCode Required. The ISO 4217 alphabetic currency code of the transaction.
 * @property totalPrice Required. The total price of this transaction in format:
 * [0-9]+(\.[0-9][0-9])? (ex: "12.34")
 * @property totalPriceStatus Required. The [GooglePayTotalPriceStatus] status of the transaction's
 * total price.
 * @property isEmailRequired Optional. Set`true` if the buyer's email address is required to be
 * returned, `false` otherwise.
 * @property isPhoneNumberRequired Optional. Set `true` if the buyer's phone number is required to
 * be returned as part of the
 * billing address and shipping address, `false` otherwise.
 * @property isBillingAddressRequired Optional. Set`true` if the buyer's billing address is required
 * to be returned, false` otherwise.
 * @property billingAddressFormat Optional. The [GooglePayBillingAddressFormat] billing address
 * format to return. Defaults to [GooglePayBillingAddressFormat.MIN].
 * @property isShippingAddressRequired Optional. Set `true` if the buyer's shipping address is
 * required to be returned, `false` otherwise.
 * @property shippingAddressParameters Optional. The shipping address requirements.
 * @property allowPrepaidCards Defaults to `false`. Set`true` prepaid cards are allowed.
 * @property isPayPalEnabled Defaults to `true`. Allows PayPal to be a payment method in Google Pay.
 * @property googleMerchantName Optional. The merchant name that will be presented in Google Pay
 * @property countryCode The ISO 3166-1 alpha-2 country code where the transaction is processed.
 * This is required for merchants based in European Economic Area (EEA) countries.
 * NOTE: to support Elo cards, country code must be set to "BR"
 * @property totalPriceLabel Optional. Custom label for the total price within the display items
 * @property allowCreditCards Defaults to `true`.
 */
@Suppress("TooManyFunctions")
@Parcelize
class GooglePayRequest @JvmOverloads constructor(
    var currencyCode: String,
    var totalPrice: String,
    var totalPriceStatus: GooglePayTotalPriceStatus,
    var isEmailRequired: Boolean = false,
    var isPhoneNumberRequired: Boolean = false,
    var isBillingAddressRequired: Boolean = false,
    var billingAddressFormat: GooglePayBillingAddressFormat? = GooglePayBillingAddressFormat.MIN,
    var isShippingAddressRequired: Boolean = false,
    var shippingAddressParameters: GooglePayShippingAddressParameters? = null,
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
        val allowedPaymentMethods = JSONArray()
        val shippingAddressParameters = JSONObject()

        if (isShippingAddressRequired) {
            val allowedCountryCodeList = this.shippingAddressParameters?.allowedCountryCodes

            if (!allowedCountryCodeList.isNullOrEmpty()) {
                try {
                    shippingAddressParameters.put(
                        "allowedCountryCodes",
                        JSONArray(allowedCountryCodeList)
                    )
                } catch (ignored: JSONException) {
                }
            }
            shippingAddressParameters.putOpt("phoneNumberRequired",
                this.shippingAddressParameters?.isPhoneNumberRequired)
        }

        transactionInfoJson.put("totalPriceStatus", totalPriceStatus.stringValue)
        transactionInfoJson.put("totalPrice", totalPrice)
        transactionInfoJson.put("currencyCode", currencyCode)
        transactionInfoJson.putOpt("countryCode", countryCode)
        transactionInfoJson.putOpt("totalPriceLabel", totalPriceLabel)

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
                                        .put("format", billingAddressFormat)
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
        merchantInfo.putOpt("merchantName", googleMerchantName)

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
