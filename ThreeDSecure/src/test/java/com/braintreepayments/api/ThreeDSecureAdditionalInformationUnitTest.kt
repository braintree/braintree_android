package com.braintreepayments.api

import android.os.Parcel
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.json.JSONException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureAdditionalInformationUnitTest {

    @Test
    fun writeToParcel() {
        val shippingAddress = ThreeDSecurePostalAddress()
        shippingAddress.givenName = "shipping-given-name"

        val preSerialized = ThreeDSecureAdditionalInformation()
        preSerialized.shippingAddress = shippingAddress
        preSerialized.shippingMethodIndicator = "shipping-method-indicator"
        preSerialized.productCode = "product_code"
        preSerialized.deliveryTimeframe = "delivery_timeframe"
        preSerialized.deliveryEmail = "delivery_email"
        preSerialized.reorderIndicator = "reorder_indicator"
        preSerialized.preorderIndicator = "preorder_indicator"
        preSerialized.preorderDate = "preorder_date"
        preSerialized.giftCardAmount = "gift_card_amount"
        preSerialized.giftCardCurrencyCode = "gift_card_currency_code"
        preSerialized.giftCardCount = "gift_card_count"
        preSerialized.accountAgeIndicator = "account_age_indicator"
        preSerialized.accountCreateDate = "account_create_date"
        preSerialized.accountChangeIndicator = "account_change_indicator"
        preSerialized.accountChangeDate = "account_change_date"
        preSerialized.accountPwdChangeIndicator = "account_pwd_change_indicator"
        preSerialized.accountPwdChangeDate = "account_pwd_change_date"
        preSerialized.shippingAddressUsageIndicator = "shipping_address_usage_indicator"
        preSerialized.shippingAddressUsageDate = "shipping_address_usage_date"
        preSerialized.transactionCountDay = "transaction_count_day"
        preSerialized.transactionCountYear = "transaction_count_year"
        preSerialized.addCardAttempts = "add_card_attempts"
        preSerialized.accountPurchases = "account_purchases"
        preSerialized.fraudActivity = "fraud_activity"
        preSerialized.shippingNameIndicator = "shipping_name_indicator"
        preSerialized.paymentAccountIndicator = "payment_account_indicator"
        preSerialized.paymentAccountAge = "payment_account_age"
        preSerialized.addressMatch = "address_match"
        preSerialized.accountId = "account_id"
        preSerialized.ipAddress = "ip_address"
        preSerialized.orderDescription = "order_description"
        preSerialized.taxAmount = "tax_amount"
        preSerialized.userAgent = "user_agent"
        preSerialized.authenticationIndicator = "authentication_indicator"
        preSerialized.installment = "installment"
        preSerialized.purchaseDate = "purchase_date"
        preSerialized.recurringEnd = "recurring_end"
        preSerialized.recurringFrequency = "recurring_frequency"
        preSerialized.sdkMaxTimeout = "06"
        preSerialized.workPhoneNumber = "5551115555"

        val parcel = Parcel.obtain()
        preSerialized.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val postSerialized = ThreeDSecureAdditionalInformation.CREATOR.createFromParcel(parcel)

        assertEquals("shipping-given-name", postSerialized.shippingAddress!!.givenName)
        assertEquals("shipping-method-indicator", postSerialized.shippingMethodIndicator)
        assertEquals("product_code", postSerialized.productCode)
        assertEquals("delivery_timeframe", postSerialized.deliveryTimeframe)
        assertEquals("delivery_email", postSerialized.deliveryEmail)
        assertEquals("reorder_indicator", postSerialized.reorderIndicator)
        assertEquals("preorder_indicator", postSerialized.preorderIndicator)
        assertEquals("preorder_date", postSerialized.preorderDate)
        assertEquals("gift_card_amount", postSerialized.giftCardAmount)
        assertEquals("gift_card_currency_code", postSerialized.giftCardCurrencyCode)
        assertEquals("gift_card_count", postSerialized.giftCardCount)
        assertEquals("account_age_indicator", postSerialized.accountAgeIndicator)
        assertEquals("account_create_date", postSerialized.accountCreateDate)
        assertEquals("account_change_indicator", postSerialized.accountChangeIndicator)
        assertEquals("account_change_date", postSerialized.accountChangeDate)
        assertEquals("account_pwd_change_indicator", postSerialized.accountPwdChangeIndicator)
        assertEquals("account_pwd_change_date", postSerialized.accountPwdChangeDate)
        assertEquals("shipping_address_usage_indicator", postSerialized.shippingAddressUsageIndicator)
        assertEquals("shipping_address_usage_date", postSerialized.shippingAddressUsageDate)
        assertEquals("transaction_count_day", postSerialized.transactionCountDay)
        assertEquals("transaction_count_year", postSerialized.transactionCountYear)
        assertEquals("add_card_attempts", postSerialized.addCardAttempts)
        assertEquals("account_purchases", postSerialized.accountPurchases)
        assertEquals("fraud_activity", postSerialized.fraudActivity)
        assertEquals("shipping_name_indicator", postSerialized.shippingNameIndicator)
        assertEquals("payment_account_indicator", postSerialized.paymentAccountIndicator)
        assertEquals("payment_account_age", postSerialized.paymentAccountAge)
        assertEquals("address_match", postSerialized.addressMatch)
        assertEquals("account_id", postSerialized.accountId)
        assertEquals("ip_address", postSerialized.ipAddress)
        assertEquals("order_description", postSerialized.orderDescription)
        assertEquals("tax_amount", postSerialized.taxAmount)
        assertEquals("user_agent", postSerialized.userAgent)
        assertEquals("authentication_indicator", postSerialized.authenticationIndicator)
        assertEquals("installment", postSerialized.installment)
        assertEquals("purchase_date", postSerialized.purchaseDate)
        assertEquals("recurring_end", postSerialized.recurringEnd)
        assertEquals("recurring_frequency", postSerialized.recurringFrequency)
        assertEquals("06", postSerialized.sdkMaxTimeout)
        assertEquals("5551115555", postSerialized.workPhoneNumber)
    }

    @Test
    @Throws(JSONException::class)
    fun toJson() {
        val shippingAddress = ThreeDSecurePostalAddress()
        shippingAddress.givenName = "shipping-given-name"
        shippingAddress.surname = "shipping-surname"
        shippingAddress.phoneNumber = "shipping-phone"
        shippingAddress.streetAddress = "shipping-line1"
        shippingAddress.extendedAddress = "shipping-line2"
        shippingAddress.line3 = "shipping-line3"
        shippingAddress.locality = "shipping-city"
        shippingAddress.region = "shipping-state"
        shippingAddress.postalCode = "shipping-postal-code"
        shippingAddress.countryCodeAlpha2 = "shipping-country-code"

        val additionalInformation = ThreeDSecureAdditionalInformation()
        additionalInformation.shippingAddress = shippingAddress
        additionalInformation.shippingMethodIndicator = "shipping-method-indicator"
        additionalInformation.productCode = "product_code"
        additionalInformation.deliveryTimeframe = "delivery_timeframe"
        additionalInformation.deliveryEmail = "delivery_email"
        additionalInformation.reorderIndicator = "reorder_indicator"
        additionalInformation.preorderIndicator = "preorder_indicator"
        additionalInformation.preorderDate = "preorder_date"
        additionalInformation.giftCardAmount = "gift_card_amount"
        additionalInformation.giftCardCurrencyCode = "gift_card_currency_code"
        additionalInformation.giftCardCount = "gift_card_count"
        additionalInformation.accountAgeIndicator = "account_age_indicator"
        additionalInformation.accountCreateDate = "account_create_date"
        additionalInformation.accountChangeIndicator = "account_change_indicator"
        additionalInformation.accountChangeDate = "account_change_date"
        additionalInformation.accountPwdChangeIndicator = "account_pwd_change_indicator"
        additionalInformation.accountPwdChangeDate = "account_pwd_change_date"
        additionalInformation.shippingAddressUsageIndicator = "shipping_address_usage_indicator"
        additionalInformation.shippingAddressUsageDate = "shipping_address_usage_date"
        additionalInformation.transactionCountDay = "transaction_count_day"
        additionalInformation.transactionCountYear = "transaction_count_year"
        additionalInformation.addCardAttempts = "add_card_attempts"
        additionalInformation.accountPurchases = "account_purchases"
        additionalInformation.fraudActivity = "fraud_activity"
        additionalInformation.shippingNameIndicator = "shipping_name_indicator"
        additionalInformation.paymentAccountIndicator = "payment_account_indicator"
        additionalInformation.paymentAccountAge = "payment_account_age"
        additionalInformation.addressMatch = "address_match"
        additionalInformation.accountId = "account_id"
        additionalInformation.ipAddress = "ip_address"
        additionalInformation.orderDescription = "order_description"
        additionalInformation.taxAmount = "tax_amount"
        additionalInformation.userAgent = "user_agent"
        additionalInformation.authenticationIndicator = "authentication_indicator"
        additionalInformation.installment = "installment"
        additionalInformation.purchaseDate = "purchase_date"
        additionalInformation.recurringEnd = "recurring_end"
        additionalInformation.recurringFrequency = "recurring_frequency"
        additionalInformation.sdkMaxTimeout = "06"
        additionalInformation.workPhoneNumber = "5551115555"

        val jsonParams = additionalInformation.toJson()

        assertEquals("shipping-given-name", jsonParams.getString("shipping_given_name"))
        assertEquals("shipping-surname", jsonParams.getString("shipping_surname"))
        assertEquals("shipping-phone", jsonParams.getString("shipping_phone"))
        assertEquals("shipping-line1", jsonParams.getString("shipping_line1"))
        assertEquals("shipping-line2", jsonParams.getString("shipping_line2"))
        assertEquals("shipping-line3", jsonParams.getString("shipping_line3"))
        assertEquals("shipping-city", jsonParams.getString("shipping_city"))
        assertEquals("shipping-state", jsonParams.getString("shipping_state"))
        assertEquals("shipping-postal-code", jsonParams.getString("shipping_postal_code"))
        assertEquals("shipping-country-code", jsonParams.getString("shipping_country_code"))
        assertEquals("shipping-method-indicator", jsonParams.getString("shipping_method_indicator"))
        assertEquals("product_code", jsonParams.getString("product_code"))
        assertEquals("delivery_timeframe", jsonParams.getString("delivery_timeframe"))
        assertEquals("delivery_email", jsonParams.getString("delivery_email"))
        assertEquals("reorder_indicator", jsonParams.getString("reorder_indicator"))
        assertEquals("preorder_indicator", jsonParams.getString("preorder_indicator"))
        assertEquals("preorder_date", jsonParams.getString("preorder_date"))
        assertEquals("gift_card_amount", jsonParams.getString("gift_card_amount"))
        assertEquals("gift_card_currency_code", jsonParams.getString("gift_card_currency_code"))
        assertEquals("gift_card_count", jsonParams.getString("gift_card_count"))
        assertEquals("account_age_indicator", jsonParams.getString("account_age_indicator"))
        assertEquals("account_create_date", jsonParams.getString("account_create_date"))
        assertEquals("account_change_indicator", jsonParams.getString("account_change_indicator"))
        assertEquals("account_change_date", jsonParams.getString("account_change_date"))
        assertEquals("account_pwd_change_indicator", jsonParams.getString("account_pwd_change_indicator"))
        assertEquals("account_pwd_change_date", jsonParams.getString("account_pwd_change_date"))
        assertEquals("shipping_address_usage_indicator", jsonParams.getString("shipping_address_usage_indicator"))
        assertEquals("shipping_address_usage_date", jsonParams.getString("shipping_address_usage_date"))
        assertEquals("transaction_count_day", jsonParams.getString("transaction_count_day"))
        assertEquals("transaction_count_year", jsonParams.getString("transaction_count_year"))
        assertEquals("add_card_attempts", jsonParams.getString("add_card_attempts"))
        assertEquals("account_purchases", jsonParams.getString("account_purchases"))
        assertEquals("fraud_activity", jsonParams.getString("fraud_activity"))
        assertEquals("shipping_name_indicator", jsonParams.getString("shipping_name_indicator"))
        assertEquals("payment_account_indicator", jsonParams.getString("payment_account_indicator"))
        assertEquals("payment_account_age", jsonParams.getString("payment_account_age"))
        assertEquals("address_match", jsonParams.getString("address_match"))
        assertEquals("account_id", jsonParams.getString("account_id"))
        assertEquals("ip_address", jsonParams.getString("ip_address"))
        assertEquals("order_description", jsonParams.getString("order_description"))
        assertEquals("tax_amount", jsonParams.getString("tax_amount"))
        assertEquals("user_agent", jsonParams.getString("user_agent"))
        assertEquals("authentication_indicator", jsonParams.getString("authentication_indicator"))
        assertEquals("installment", jsonParams.getString("installment"))
        assertEquals("purchase_date", jsonParams.getString("purchase_date"))
        assertEquals("recurring_end", jsonParams.getString("recurring_end"))
        assertEquals("recurring_frequency", jsonParams.getString("recurring_frequency"))
        assertEquals("06", jsonParams.getString("sdk_max_timeout"))
        assertEquals("5551115555", jsonParams.getString("work_phone_number"))
    }

    @Test
    fun testToJson_buildsEmptyParameters() {
        val additionalInformation = ThreeDSecureAdditionalInformation()
        val jsonParams = additionalInformation.toJson()

        assertTrue(jsonParams.isNull("shipping_given_name"))
        assertTrue(jsonParams.isNull("billingAddress"))
        assertTrue(jsonParams.isNull("firstName"))
        assertTrue(jsonParams.isNull("lastName"))
        assertTrue(jsonParams.isNull("shippingMethod"))
        assertTrue(jsonParams.isNull("mobilePhoneNumber"))
        assertTrue(jsonParams.isNull("shipping_method_indicator"))
        assertTrue(jsonParams.isNull("product_code"))
        assertTrue(jsonParams.isNull("delivery_timeframe"))
        assertTrue(jsonParams.isNull("delivery_email"))
        assertTrue(jsonParams.isNull("reorder_indicator"))
        assertTrue(jsonParams.isNull("preorder_indicator"))
        assertTrue(jsonParams.isNull("preorder_date"))
        assertTrue(jsonParams.isNull("gift_card_amount"))
        assertTrue(jsonParams.isNull("gift_card_currency_code"))
        assertTrue(jsonParams.isNull("gift_card_count"))
        assertTrue(jsonParams.isNull("account_age_indicator"))
        assertTrue(jsonParams.isNull("account_create_date"))
        assertTrue(jsonParams.isNull("account_change_indicator"))
        assertTrue(jsonParams.isNull("account_change_date"))
        assertTrue(jsonParams.isNull("account_pwd_change_indicator"))
        assertTrue(jsonParams.isNull("account_pwd_change_date"))
        assertTrue(jsonParams.isNull("shipping_address_usage_indicator"))
        assertTrue(jsonParams.isNull("shipping_address_usage_date"))
        assertTrue(jsonParams.isNull("transaction_count_day"))
        assertTrue(jsonParams.isNull("transaction_count_year"))
        assertTrue(jsonParams.isNull("add_card_attempts"))
        assertTrue(jsonParams.isNull("account_purchases"))
        assertTrue(jsonParams.isNull("fraud_activity"))
        assertTrue(jsonParams.isNull("shipping_name_indicator"))
        assertTrue(jsonParams.isNull("payment_account_indicator"))
        assertTrue(jsonParams.isNull("payment_account_age"))
        assertTrue(jsonParams.isNull("address_match"))
        assertTrue(jsonParams.isNull("account_id"))
        assertTrue(jsonParams.isNull("ip_address"))
        assertTrue(jsonParams.isNull("order_description"))
        assertTrue(jsonParams.isNull("tax_amount"))
        assertTrue(jsonParams.isNull("user_agent"))
        assertTrue(jsonParams.isNull("authentication_indicator"))
        assertTrue(jsonParams.isNull("installment"))
        assertTrue(jsonParams.isNull("purchase_date"))
        assertTrue(jsonParams.isNull("recurring_end"))
        assertTrue(jsonParams.isNull("recurring_frequency"))
        assertTrue(jsonParams.isNull("sdk_max_timeout"))
        assertTrue(jsonParams.isNull("work_phone_number"))
    }
}