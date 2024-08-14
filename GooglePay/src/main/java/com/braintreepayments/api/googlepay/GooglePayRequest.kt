package com.braintreepayments.api.googlepay

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.google.android.gms.wallet.ShippingAddressRequirements
import com.google.android.gms.wallet.TransactionInfo
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.WalletConstants.BillingAddressFormat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

/**
 * Represents the parameters that are needed to use the Google Pay API.
 */
@SuppressWarnings("TooManyFunctions")
class GooglePayRequest : Parcelable {
    /**
     * Details and the price of the transaction. Required.
     *
     * @return See [TransactionInfo].
     */
    /**
     * Details and the price of the transaction. Required.
     *
     * @param transactionInfo See [TransactionInfo].
     */
    // NEXT_MAJOR_VERSION: allow merchants to set transaction info params individually and build
    // JSON object under the hood
    var transactionInfo: TransactionInfo? = null
    /**
     * @return If the buyer's email address is required to be returned.
     */
    /**
     * Optional.
     *
     * @param emailRequired `true` if the buyer's email address is required to be returned, `false` otherwise.
     */
    var isEmailRequired: Boolean = false
    /**
     * @return If the buyer's phone number is required to be returned as part of the
     * billing address and shipping address.
     */
    /**
     * Optional.
     *
     * @param phoneNumberRequired `true` if the buyer's phone number is required to be returned as part of the
     * billing address and shipping address, `false` otherwise.
     */
    var isPhoneNumberRequired: Boolean = false
    /**
     * @return If the buyer's billing address is required to be returned.
     */
    /**
     * Optional.
     *
     * @param billingAddressRequired `true` if the buyer's billing address is required to be returned,
     * `false` otherwise.
     */
    var isBillingAddressRequired: Boolean = false
    /**
     * @return If the buyer's billing address is required to be returned.
     */
    /**
     * Optional.
     *
     * @param billingAddressFormat the billing address format to return. [BillingAddressFormat]
     */
    @get:BillingAddressFormat
    var billingAddressFormat: Int = 0
    /**
     * @return If the buyer's shipping address is required to be returned.
     */
    /**
     * Optional.
     *
     * @param shippingAddressRequired `true` if the buyer's shipping address is required to be returned,
     * `false` otherwise.
     */
    var isShippingAddressRequired: Boolean = false
    /**
     * @return The shipping address requirements. See [ShippingAddressRequirements].
     */
    /**
     * Optional.
     *
     * @param shippingAddressRequirements the shipping address requirements. [ShippingAddressRequirements]
     */
    // NEXT_MAJOR_VERSION: allow merchants to set shipping address requirements params individually
    // and build JSON object under the hood
    var shippingAddressRequirements: ShippingAddressRequirements? = null
    /**
     * @return If prepaid cards are allowed.
     */
    /**
     * Optional.
     *
     * @param allowPrepaidCards `true` prepaid cards are allowed, `false` otherwise.
     */
    var allowPrepaidCards: Boolean = false
    /**
     * @return If PayPal should be an available payment method in Google Pay.
     */
    /**
     * Defines if PayPal should be an available payment method in Google Pay.
     * Defaults to `true`.
     *
     * @param enablePayPal `true` by default. Allows PayPal to be a payment method in Google Pay.
     */
    var isPayPalEnabled: Boolean = true
    private val allowedPaymentMethods = HashMap<String, JSONObject>()
    private val tokenizationSpecifications = HashMap<String, JSONObject>()
    private val allowedAuthMethods = HashMap<String, JSONArray>()
    private val allowedCardNetworks = HashMap<String, JSONArray>()
    private var environment: String? = null

    /**
     * @return If credit cards are allowed.
     */
    var isCreditCardsAllowed: Boolean = true
        private set
    /**
     * @return The merchant name that will be presented in Google Pay.
     */
    /**
     * Optional.
     *
     * @param merchantName The merchant name that will be presented in Google Pay
     */
    var googleMerchantName: String? = null
    /**
     * @return The country code where the transaction is processed.
     */
    /**
     * ISO 3166-1 alpha-2 country code where the transaction is processed. This is required for
     * merchants based in European Economic Area (EEA) countries.
     *
     *
     * NOTE: to support Elo cards, country code must be set to "BR"
     *
     * @param countryCode The country code where the transaction is processed
     */
    var countryCode: String? = null

    /**
     * Optional
     *
     * @param totalPriceLabel Custom label for the total price within the display items
     */
    var totalPriceLabel: String? = null

    constructor()

    /**
     * Simple wrapper to assign given parameters to specified paymentMethod
     *
     * @param paymentMethodType The paymentMethod to add to
     * @param parameters        Parameters to assign to the paymentMethod
     */
    fun setAllowedPaymentMethod(paymentMethodType: String, parameters: JSONObject) {
        allowedPaymentMethods[paymentMethodType] = parameters
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's tokenizationSpecification
     *
     * @param paymentMethodType The paymentMethod to attached tokenizationSpecification parameters to
     * @param parameters        The tokenizationSpecification parameters to attach
     */
    fun setTokenizationSpecificationForType(paymentMethodType: String, parameters: JSONObject) {
        tokenizationSpecifications[paymentMethodType] = parameters
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's allowedAuthMethods
     *
     * @param paymentMethodType the paymentMethod to attach allowedAuthMethods to
     * @param authMethods       the authMethods to allow the paymentMethodType to transact with
     */
    fun setAllowedAuthMethods(paymentMethodType: String, authMethods: JSONArray) {
        allowedAuthMethods[paymentMethodType] = authMethods
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's cardNetworks
     *
     * @param paymentMethodType the paymentMethod to attach cardNetworks to
     * @param cardNetworks      the cardNetworks to allow the paymentMethodType to transact with
     */
    fun setAllowedCardNetworks(paymentMethodType: String, cardNetworks: JSONArray) {
        allowedCardNetworks[paymentMethodType] = cardNetworks
    }

    fun setEnvironment(environment: String?) {
        this.environment =
            if ("PRODUCTION" == environment?.uppercase(Locale.getDefault())) "PRODUCTION" else "TEST"
    }

    /**
     * Optional.
     *
     * @param allowCreditCards Set to `false` if you don't support credit cards.
     * Defaults to `true`.
     */
    fun setAllowCreditCards(allowCreditCards: Boolean) {
        this.isCreditCardsAllowed = allowCreditCards
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
            allowedCountryCodeList = shippingAddressRequirements!!.allowedCountryCodes

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
                .put("totalPrice", transactionInfo!!.totalPrice)
                .put("currencyCode", transactionInfo.currencyCode)

            if (countryCode != null) {
                transactionInfoJson.put("countryCode", countryCode)
            }

            if (totalPriceLabel != null) {
                transactionInfoJson.put("totalPriceLabel", totalPriceLabel)
            }
        } catch (ignored: JSONException) {
        }

        for ((key, value) in this.allowedPaymentMethods) {
            try {
                val paymentMethod = JSONObject()
                    .put("type", key)
                    .put("parameters", value)
                    .put("tokenizationSpecification", tokenizationSpecifications[key])

                if ("CARD" == key) {
                    val paymentMethodParams = paymentMethod.getJSONObject("parameters")
                    paymentMethodParams
                        .put("billingAddressRequired", isBillingAddressRequired)
                        .put("allowPrepaidCards", allowPrepaidCards)
                        .put("allowCreditCards", isCreditCardsAllowed)
                    try {
                        val billingAddressParameters =
                            value["billingAddressParameters"] as JSONObject
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
        return when (transactionInfo!!.totalPriceStatus) {
            WalletConstants.TOTAL_PRICE_STATUS_NOT_CURRENTLY_KNOWN -> "NOT_CURRENTLY_KNOWN"
            WalletConstants.TOTAL_PRICE_STATUS_ESTIMATED -> "ESTIMATED"
            WalletConstants.TOTAL_PRICE_STATUS_FINAL -> "FINAL"
            else -> "FINAL"
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
        return allowedPaymentMethods[type]
    }

    /**
     * @return Tokenization specification for a given payment method type.
     */
    fun getTokenizationSpecificationForType(type: String): JSONObject? {
        return tokenizationSpecifications[type]
    }

    /**
     * @return Allowed authentication methods for a given payment method type.
     */
    fun getAllowedAuthMethodsForType(type: String): JSONArray? {
        return allowedAuthMethods[type]
    }

    /**
     * @return Allowed card networks for a given payment method type.
     */
    fun getAllowedCardNetworksForType(type: String): JSONArray? {
        return allowedCardNetworks[type]
    }

    fun getEnvironment(): String? {
        return environment
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(transactionInfo, flags)
        dest.writeByte((if (isEmailRequired) 1 else 0).toByte())
        dest.writeByte((if (isPhoneNumberRequired) 1 else 0).toByte())
        dest.writeByte((if (isBillingAddressRequired) 1 else 0).toByte())
        dest.writeInt(billingAddressFormat)
        dest.writeByte((if (isShippingAddressRequired) 1 else 0).toByte())
        dest.writeParcelable(shippingAddressRequirements, flags)
        dest.writeByte((if (allowPrepaidCards) 1 else 0).toByte())
        dest.writeByte((if (isPayPalEnabled) 1 else 0).toByte())
        dest.writeString(environment)
        dest.writeString(googleMerchantName)
        dest.writeString(countryCode)
        dest.writeByte((if (isCreditCardsAllowed) 1 else 0).toByte())
        dest.writeString(totalPriceLabel)
    }

    internal constructor(`in`: Parcel) {
        transactionInfo = `in`.readParcelable(TransactionInfo::class.java.classLoader)
        isEmailRequired = `in`.readByte().toInt() != 0
        isPhoneNumberRequired = `in`.readByte().toInt() != 0
        isBillingAddressRequired = `in`.readByte().toInt() != 0
        billingAddressFormat = `in`.readInt()
        isShippingAddressRequired = `in`.readByte().toInt() != 0
        shippingAddressRequirements = `in`.readParcelable(
            ShippingAddressRequirements::class.java.classLoader
        )
        allowPrepaidCards = `in`.readByte().toInt() != 0
        isPayPalEnabled = `in`.readByte().toInt() != 0
        environment = `in`.readString()
        googleMerchantName = `in`.readString()
        countryCode = `in`.readString()
        isCreditCardsAllowed = `in`.readByte().toInt() != 0
        totalPriceLabel = `in`.readString()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<GooglePayRequest> =
            object : Parcelable.Creator<GooglePayRequest> {
                override fun createFromParcel(`in`: Parcel): GooglePayRequest {
                    return GooglePayRequest(`in`)
                }

                override fun newArray(size: Int): Array<GooglePayRequest?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
