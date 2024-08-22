package com.braintreepayments.api.threedsecure

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * A class containing additional information for ThreeDSecure 2.0 Requests
 *
 * @property shippingAddress The shipping address used for verification.
 *
 * @property shippingMethodIndicator The 2-digit string indicating the shipping method chosen for
 * the transaction
 *
 * Possible Values: 01 Ship to cardholder billing address 02 Ship to another verified address on
 * file with merchant 03 Ship to address that is different than billing address 04 Ship to store
 * (store address should be populated on request) 05 Digital goods 06 Travel and event tickets,
 * not shipped 07 Other
 *
 * @property productCode The 3-letter string representing the merchant product code
 *
 * Possible Values: AIR Airline GEN General Retail DIG Digital Goods SVC Services RES Restaurant
 * TRA Travel DSP Cash Dispensing REN Car Rental GAS Fueld LUX Luxury Retail ACC Accommodation
 * Retail TBD Other
 *
 * @property deliveryTimeframe Optional. The 2-digit number indicating the delivery timeframe
 *
 * Possible values: 01 Electronic delivery 02 Same day shipping 03 Overnight shipping 04 Two or
 * more day shipping
 *
 * @property deliveryEmail For electronic delivery, email address to which the merchandise was
 * delivered
 *
 * @property reorderIndicator The 2-digit number indicating whether the cardholder is reordering
 * previously purchased merchandise
 *
 * Possible values: 01 First time ordered 02 Reordered
 *
 * @property preorderIndicator The 2-digit number indicating whether the cardholder is placing an
 * order with a future availability or release date
 *
 * Possible values: 01 Merchandise available 02 Future availability
 *
 * @property preorderDate The 8-digit number (format: YYYYMMDD) indicating expected date that a
 * pre-ordered purchase will be available
 *
 * @property giftCardAmount The purchase amount total for prepaid gift cards in major units
 *
 * @property giftCardCurrencyCode ISO 4217 currency code for the gift card purchased
 *
 * @property giftCardCount Total count of individual prepaid gift cards purchased
 *
 * @property accountAgeIndicator The 2-digit value representing the length of time cardholder has
 * had account.
 *
 * Possible values: 01 No account 02 Created during transaction 03 Less than 30 days 04 30-60
 * days 05 More than 60 days
 *
 * @property accountCreateDate The 8-digit number (format: YYYYMMDD) indicating the date the
 * cardholder opened the account.
 *
 * @property accountChangeIndicator Optional. The 2-digit value representing the length of time
 * since the last change to the cardholder account. This includes shipping address, new payment
 * account or new user added.
 *
 * Possible values: 01 Changed during transaction 02 Less than 30 days 03 30-60 days 04 More
 * than 60 days
 *
 * @property accountChangeDate The 8-digit number (format: YYYYMMDD) indicating the date the
 * cardholder's account was last changed. This includes changes to the billing or shipping address,
 * new payment accounts or new users added.
 *
 * @property accountPwdChangeIndicator The 2-digit value representing the length of time since the
 * cardholder changed or reset the password on the account.
 *
 * Possible values: 01 No change 02 Changed during transaction 03 Less than 30 days 04 30-60
 * days 05 More than 60 days
 *
 * @property accountPwdChangeDate The 8-digit number (format: YYYYMMDD) indicating the date the
 * cardholder last changed or reset password on account.
 *
 * @property shippingAddressUsageIndicator The 2-digit value indicating when the shipping address
 * used for transaction was first used.
 *
 * Possible values: 01 This transaction 02 Less than 30 days 03 30-60 days 04 More than 60 days
 *
 * @property shippingAddressUsageDate The 8-digit number (format: YYYYMMDD) indicating the date when
 * the shipping address used for this transaction was first used.
 *
 * @property transactionCountDay Number of transactions (successful or abandoned) for this
 * cardholder account within the last 24 hours.
 *
 * @property transactionCountYear Number of transactions (successful or abandoned) for this
 * cardholder account within the last year.
 *
 * @property addCardAttempts Number of add card attempts in the last 24 hours.
 *
 * @property accountPurchases Number of purchases with this cardholder account during the previous
 * six months.
 *
 * @property fraudActivity The 2-digit value indicating whether the merchant experienced suspicious
 * activity (including previous fraud) on the account.
 *
 * Possible values: 01 No suspicious activity 02 Suspicious activity observed
 *
 * @property shippingNameIndicator The 2-digit value indicating if the cardholder name on the
 * account is identical to the shipping name used for the transaction.
 *
 * Possible values: 01 Account name identical to shipping name 02 Account name different than
 * shipping name
 *
 * @property paymentAccountIndicator The 2-digit value indicating the length of time that the
 * payment account was enrolled in the merchant account.
 *
 * Possible values: 01 No account (guest checkout) 02 During the transaction 03 Less than 30
 * days 04 30-60 days 05 More than 60 days
 *
 * @property paymentAccountAge The 8-digit number (format: YYYYMMDD) indicating the date the payment
 * account was added to the cardholder account.
 *
 * @property addressMatch The 1-character value (Y/N) indicating whether cardholder billing and
 * shipping addresses match.
 *
 * @property accountId Additional cardholder account information.
 *
 * @property ipAddress The IP address of the consumer. IPv4 and IPv6 are supported.
 *
 * @property orderDescription Brief description of items purchased.
 *
 * @property taxAmount Unformatted tax amount without any decimalization (ie. $123.67 = 12367).
 *
 * @property userAgent The exact content of the HTTP user agent header.
 *
 * @property authenticationIndicator The 2-digit number indicating the type of authentication
 * request.
 *
 * Possible values: 02 Recurring transaction 03 Installment transaction
 *
 * @property installment An integer value greater than 1 indicating the maximum number of permitted
 * authorizations for installment payments.
 *
 * @property purchaseDate The 14-digit number (format: YYYYMMDDHHMMSS) indicating the date in UTC of
 * original purchase.
 *
 * @property recurringEnd The 8-digit number (format: YYYYMMDD) indicating the date after which no
 * further recurring authorizations should be performed.
 *
 * @property recurringFrequency Integer value indicating the minimum number of days between
 * recurring authorizations. A frequency of monthly is indicated by the value 28. Multiple of 28
 * days will be used to indicate months (ex. 6 months = 168).
 *
 * @property sdkMaxTimeout The 2-digit number of minutes (minimum 05) to set the maximum amount of
 * time for all 3DS 2.0 messages to be communicated between all components.
 *
 * @property workPhoneNumber The work phone number used for verification. Only numbers; remove
 * dashes, parenthesis and other characters.
 */
@Parcelize
data class ThreeDSecureAdditionalInformation(
    var shippingAddress: ThreeDSecurePostalAddress? = null,
    var shippingMethodIndicator: String? = null,
    var productCode: String? = null,
    var deliveryTimeframe: String? = null,
    var deliveryEmail: String? = null,
    var reorderIndicator: String? = null,
    var preorderIndicator: String? = null,
    var preorderDate: String? = null,
    var giftCardAmount: String? = null,
    var giftCardCurrencyCode: String? = null,
    var giftCardCount: String? = null,
    var accountAgeIndicator: String? = null,
    var accountCreateDate: String? = null,
    var accountChangeIndicator: String? = null,
    var accountChangeDate: String? = null,
    var accountPwdChangeIndicator: String? = null,
    var accountPwdChangeDate: String? = null,
    var shippingAddressUsageIndicator: String? = null,
    var shippingAddressUsageDate: String? = null,
    var transactionCountDay: String? = null,
    var transactionCountYear: String? = null,
    var addCardAttempts: String? = null,
    var accountPurchases: String? = null,
    var fraudActivity: String? = null,
    var shippingNameIndicator: String? = null,
    var paymentAccountIndicator: String? = null,
    var paymentAccountAge: String? = null,
    var addressMatch: String? = null,
    var accountId: String? = null,
    var ipAddress: String? = null,
    var orderDescription: String? = null,
    var taxAmount: String? = null,
    var userAgent: String? = null,
    var authenticationIndicator: String? = null,
    var installment: String? = null,
    var purchaseDate: String? = null,
    var recurringEnd: String? = null,
    var recurringFrequency: String? = null,
    var sdkMaxTimeout: String? = null,
    var workPhoneNumber: String? = null
) : Parcelable {

    /**
     * @return JSONObject representation of [ThreeDSecureAdditionalInformation].
     */
    @Suppress("LongMethod")
    fun toJson(): JSONObject {
        val additionalInformation = JSONObject()
        try {
            shippingAddress?.let {
                additionalInformation.putOpt("shipping_given_name", it.givenName)
                additionalInformation.putOpt("shipping_surname", it.surname)
                additionalInformation.putOpt("shipping_phone", it.phoneNumber)
                additionalInformation.putOpt("shipping_line1", it.streetAddress)
                additionalInformation.putOpt("shipping_line2", it.extendedAddress)
                additionalInformation.putOpt("shipping_line3", it.line3)
                additionalInformation.putOpt("shipping_city", it.locality)
                additionalInformation.putOpt("shipping_state", it.region)
                additionalInformation.putOpt("shipping_postal_code", it.postalCode)
                additionalInformation.putOpt("shipping_country_code", it.countryCodeAlpha2)
            }

            additionalInformation.putOpt("shipping_method_indicator", shippingMethodIndicator)
            additionalInformation.putOpt("product_code", productCode)
            additionalInformation.putOpt("delivery_timeframe", deliveryTimeframe)
            additionalInformation.putOpt("delivery_email", deliveryEmail)
            additionalInformation.putOpt("reorder_indicator", reorderIndicator)
            additionalInformation.putOpt("preorder_indicator", preorderIndicator)
            additionalInformation.putOpt("preorder_date", preorderDate)
            additionalInformation.putOpt("gift_card_amount", giftCardAmount)
            additionalInformation.putOpt("gift_card_currency_code", giftCardCurrencyCode)
            additionalInformation.putOpt("gift_card_count", giftCardCount)
            additionalInformation.putOpt("account_age_indicator", accountAgeIndicator)
            additionalInformation.putOpt("account_create_date", accountCreateDate)
            additionalInformation.putOpt("account_change_indicator", accountChangeIndicator)
            additionalInformation.putOpt("account_change_date", accountChangeDate)
            additionalInformation.putOpt("account_pwd_change_indicator", accountPwdChangeIndicator)
            additionalInformation.putOpt("account_pwd_change_date", accountPwdChangeDate)
            additionalInformation.putOpt(
                "shipping_address_usage_indicator",
                shippingAddressUsageIndicator
            )
            additionalInformation.putOpt("shipping_address_usage_date", shippingAddressUsageDate)
            additionalInformation.putOpt("transaction_count_day", transactionCountDay)
            additionalInformation.putOpt("transaction_count_year", transactionCountYear)
            additionalInformation.putOpt("add_card_attempts", addCardAttempts)
            additionalInformation.putOpt("account_purchases", accountPurchases)
            additionalInformation.putOpt("fraud_activity", fraudActivity)
            additionalInformation.putOpt("shipping_name_indicator", shippingNameIndicator)
            additionalInformation.putOpt("payment_account_indicator", paymentAccountIndicator)
            additionalInformation.putOpt("payment_account_age", paymentAccountAge)
            additionalInformation.putOpt("address_match", addressMatch)
            additionalInformation.putOpt("account_id", accountId)
            additionalInformation.putOpt("ip_address", ipAddress)
            additionalInformation.putOpt("order_description", orderDescription)
            additionalInformation.putOpt("tax_amount", taxAmount)
            additionalInformation.putOpt("user_agent", userAgent)
            additionalInformation.putOpt("authentication_indicator", authenticationIndicator)
            additionalInformation.putOpt("installment", installment)
            additionalInformation.putOpt("purchase_date", purchaseDate)
            additionalInformation.putOpt("recurring_end", recurringEnd)
            additionalInformation.putOpt("recurring_frequency", recurringFrequency)
            additionalInformation.putOpt("sdk_max_timeout", sdkMaxTimeout)
            additionalInformation.putOpt("work_phone_number", workPhoneNumber)
        } catch (ignored: JSONException) {
        }

        return additionalInformation
    }
}
