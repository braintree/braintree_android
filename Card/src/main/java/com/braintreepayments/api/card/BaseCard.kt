package com.braintreepayments.api.card

import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.PaymentMethod
import org.json.JSONException
import org.json.JSONObject

/**
 * Base class used to build various types of cards
 */
abstract class BaseCard(
    open val cardholderName: String? = null,
    open val number: String? = null,
    open val company: String? = null,
    open val countryCode: String? = null,
    open val cvv: String? = null,
    open var expirationMonth: String? = null,
    open var expirationYear: String? = null,
    open val extendedAddress: String? = null,
    open val firstName: String? = null,
    open val lastName: String? = null,
    open val locality: String? = null,
    open val postalCode: String? = null,
    open val region: String? = null,
    open val streetAddress: String? = null,
    override var integration: String? = null,
    override var source: String? = null,
    override var sessionId: String? = null,
    override var apiPath: String = "credit_cards"
) : PaymentMethod(integration, source, sessionId, apiPath) {

    companion object {
        const val BILLING_ADDRESS_KEY: String = "billingAddress"
        const val CARDHOLDER_NAME_KEY: String = "cardholderName"
        const val COMPANY_KEY: String = "company"
        const val COUNTRY_CODE_ALPHA3_KEY: String = "countryCodeAlpha3"
        const val COUNTRY_CODE_KEY: String = "countryCode"
        const val CREDIT_CARD_KEY: String = "creditCard"
        const val CVV_KEY: String = "cvv"
        const val EXPIRATION_MONTH_KEY: String = "expirationMonth"
        const val EXPIRATION_YEAR_KEY: String = "expirationYear"
        const val EXTENDED_ADDRESS_KEY: String = "extendedAddress"
        const val FIRST_NAME_KEY: String = "firstName"
        const val LAST_NAME_KEY: String = "lastName"
        const val LOCALITY_KEY: String = "locality"
        const val NUMBER_KEY: String = "number"
        const val POSTAL_CODE_KEY: String = "postalCode"
        const val REGION_KEY: String = "region"
        const val STREET_ADDRESS_KEY: String = "streetAddress"
    }

    var expirationDate: String?
        get() {
            if (expirationMonth?.isNotEmpty() == true && expirationYear?.isNotEmpty() == null) {
                return "$expirationMonth/$expirationYear"
            }
            return null
        }
        /**
         * @param expirationDate The expiration date of the card. May be in the form MM/YY or MM/YYYY.
         */
        set(expirationDate) {
            if (expirationDate.isNullOrEmpty()) {
                expirationMonth = null
                expirationYear = null
            } else {
                val splitExpiration =
                    expirationDate.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()

                expirationMonth = splitExpiration[0]

                if (splitExpiration.size > 1) {
                    expirationYear = splitExpiration[1]
                }
            }
        }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Throws(JSONException::class)
    override fun buildJSON(): JSONObject {

        val paymentMethodNonceJson = JSONObject()
        paymentMethodNonceJson.put(NUMBER_KEY, number)
        paymentMethodNonceJson.put(CVV_KEY, cvv)
        paymentMethodNonceJson.put(EXPIRATION_MONTH_KEY, expirationMonth)
        paymentMethodNonceJson.put(EXPIRATION_YEAR_KEY, expirationYear)

        paymentMethodNonceJson.put(CARDHOLDER_NAME_KEY, cardholderName)

        val billingAddressJson = JSONObject()
        billingAddressJson.put(FIRST_NAME_KEY, firstName)
        billingAddressJson.put(LAST_NAME_KEY, lastName)
        billingAddressJson.put(COMPANY_KEY, company)
        billingAddressJson.put(LOCALITY_KEY, locality)
        billingAddressJson.put(POSTAL_CODE_KEY, postalCode)
        billingAddressJson.put(REGION_KEY, region)
        billingAddressJson.put(STREET_ADDRESS_KEY, streetAddress)
        billingAddressJson.put(EXTENDED_ADDRESS_KEY, extendedAddress)

        if (countryCode != null) {
            billingAddressJson.put(COUNTRY_CODE_ALPHA3_KEY, countryCode)
        }

        if (billingAddressJson.length() > 0) {
            paymentMethodNonceJson.put(BILLING_ADDRESS_KEY, billingAddressJson)
        }

        return super.buildJSON().apply {
            put(CREDIT_CARD_KEY, paymentMethodNonceJson)
        }
    }
}
