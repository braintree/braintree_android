package com.braintreepayments.api.threedsecure

import android.os.Parcel
import kotlinx.parcelize.parcelableCreator
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SuppressWarnings("LongMethod")
@RunWith(RobolectricTestRunner::class)
class ThreeDSecureAdditionalInformationUnitTest {

    @Test
    fun `write to parcel`() {
        val shippingAddress = ThreeDSecurePostalAddress(
            givenName = "shipping-given-name"
        )

        val sut = ThreeDSecureAdditionalInformation(
            shippingAddress = shippingAddress,
            shippingMethodIndicator = "shipping-method-indicator",
            productCode = "product_code",
            deliveryTimeframe = "delivery_timeframe",
            deliveryEmail = "delivery_email",
            reorderIndicator = "reorder_indicator",
            preorderIndicator = "preorder_indicator",
            preorderDate = "preorder_date",
            giftCardAmount = "gift_card_amount",
            giftCardCurrencyCode = "gift_card_currency_code",
            giftCardCount = "gift_card_count",
            accountAgeIndicator = "account_age_indicator",
            accountCreateDate = "account_create_date",
            accountChangeIndicator = "account_change_indicator",
            accountChangeDate = "account_change_date",
            accountPwdChangeIndicator = "account_pwd_change_indicator",
            accountPwdChangeDate = "account_pwd_change_date",
            shippingAddressUsageIndicator = "shipping_address_usage_indicator",
            shippingAddressUsageDate = "shipping_address_usage_date",
            transactionCountDay = "transaction_count_day",
            transactionCountYear = "transaction_count_year",
            addCardAttempts = "add_card_attempts",
            accountPurchases = "account_purchases",
            fraudActivity = "fraud_activity",
            shippingNameIndicator = "shipping_name_indicator",
            paymentAccountIndicator = "payment_account_indicator",
            paymentAccountAge = "payment_account_age",
            addressMatch = "address_match",
            accountId = "account_id",
            ipAddress = "ip_address",
            orderDescription = "order_description",
            taxAmount = "tax_amount",
            userAgent = "user_agent",
            authenticationIndicator = "authentication_indicator",
            installment = "installment",
            purchaseDate = "purchase_date",
            recurringEnd = "recurring_end",
            recurringFrequency = "recurring_frequency",
            sdkMaxTimeout = "06",
            workPhoneNumber = "5551115555"
        )

        val parcel = Parcel.obtain().apply {
            sut.writeToParcel(this, 0)
            setDataPosition(0)
        }
        val result = parcelableCreator<ThreeDSecureAdditionalInformation>().createFromParcel(parcel)

        assertEquals("shipping-given-name", result.shippingAddress?.givenName)
        assertEquals("shipping-method-indicator", result.shippingMethodIndicator)
        assertEquals("product_code", result.productCode)
        assertEquals("delivery_timeframe", result.deliveryTimeframe)
        assertEquals("delivery_email", result.deliveryEmail)
        assertEquals("reorder_indicator", result.reorderIndicator)
        assertEquals("preorder_indicator", result.preorderIndicator)
        assertEquals("preorder_date", result.preorderDate)
        assertEquals("gift_card_amount", result.giftCardAmount)
        assertEquals("gift_card_currency_code", result.giftCardCurrencyCode)
        assertEquals("gift_card_count", result.giftCardCount)
        assertEquals("account_age_indicator", result.accountAgeIndicator)
        assertEquals("account_create_date", result.accountCreateDate)
        assertEquals("account_change_indicator", result.accountChangeIndicator)
        assertEquals("account_change_date", result.accountChangeDate)
        assertEquals("account_pwd_change_indicator", result.accountPwdChangeIndicator)
        assertEquals("account_pwd_change_date", result.accountPwdChangeDate)
        assertEquals("shipping_address_usage_indicator", result.shippingAddressUsageIndicator)
        assertEquals("shipping_address_usage_date", result.shippingAddressUsageDate)
        assertEquals("transaction_count_day", result.transactionCountDay)
        assertEquals("transaction_count_year", result.transactionCountYear)
        assertEquals("add_card_attempts", result.addCardAttempts)
        assertEquals("account_purchases", result.accountPurchases)
        assertEquals("fraud_activity", result.fraudActivity)
        assertEquals("shipping_name_indicator", result.shippingNameIndicator)
        assertEquals("payment_account_indicator", result.paymentAccountIndicator)
        assertEquals("payment_account_age", result.paymentAccountAge)
        assertEquals("address_match", result.addressMatch)
        assertEquals("account_id", result.accountId)
        assertEquals("ip_address", result.ipAddress)
        assertEquals("order_description", result.orderDescription)
        assertEquals("tax_amount", result.taxAmount)
        assertEquals("user_agent", result.userAgent)
        assertEquals("authentication_indicator", result.authenticationIndicator)
        assertEquals("installment", result.installment)
        assertEquals("purchase_date", result.purchaseDate)
        assertEquals("recurring_end", result.recurringEnd)
        assertEquals("recurring_frequency", result.recurringFrequency)
        assertEquals("06", result.sdkMaxTimeout)
        assertEquals("5551115555", result.workPhoneNumber)
    }

    @Test
    fun `to JSON`() {
        val shippingAddress = ThreeDSecurePostalAddress(
            givenName = "shipping-given-name",
            surname = "shipping-surname",
            phoneNumber = "shipping-phone",
            streetAddress = "shipping-line1",
            extendedAddress = "shipping-line2",
            line3 = "shipping-line3",
            locality = "shipping-city",
            region = "shipping-state",
            postalCode = "shipping-postal-code",
            countryCodeAlpha2 = "shipping-country-code"
        )

        val sut = ThreeDSecureAdditionalInformation(
            shippingAddress = shippingAddress,
            shippingMethodIndicator = "shipping-method-indicator",
            productCode = "product_code",
            deliveryTimeframe = "delivery_timeframe",
            deliveryEmail = "delivery_email",
            reorderIndicator = "reorder_indicator",
            preorderIndicator = "preorder_indicator",
            preorderDate = "preorder_date",
            giftCardAmount = "gift_card_amount",
            giftCardCurrencyCode = "gift_card_currency_code",
            giftCardCount = "gift_card_count",
            accountAgeIndicator = "account_age_indicator",
            accountCreateDate = "account_create_date",
            accountChangeIndicator = "account_change_indicator",
            accountChangeDate = "account_change_date",
            accountPwdChangeIndicator = "account_pwd_change_indicator",
            accountPwdChangeDate = "account_pwd_change_date",
            shippingAddressUsageIndicator = "shipping_address_usage_indicator",
            shippingAddressUsageDate = "shipping_address_usage_date",
            transactionCountDay = "transaction_count_day",
            transactionCountYear = "transaction_count_year",
            addCardAttempts = "add_card_attempts",
            accountPurchases = "account_purchases",
            fraudActivity = "fraud_activity",
            shippingNameIndicator = "shipping_name_indicator",
            paymentAccountIndicator = "payment_account_indicator",
            paymentAccountAge = "payment_account_age",
            addressMatch = "address_match",
            accountId = "account_id",
            ipAddress = "ip_address",
            orderDescription = "order_description",
            taxAmount = "tax_amount",
            userAgent = "user_agent",
            authenticationIndicator = "authentication_indicator",
            installment = "installment",
            purchaseDate = "purchase_date",
            recurringEnd = "recurring_end",
            recurringFrequency = "recurring_frequency",
            sdkMaxTimeout = "06",
            workPhoneNumber = "5551115555"
        )
        val jsonParams = sut.toJson()

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
    fun `test ToJson builds empty parameters`() {
        val additionalInformation = ThreeDSecureAdditionalInformation()

        val jsonParams = additionalInformation.toJson()

        assertTrue { jsonParams.isNull("shipping_given_name") }
        assertTrue { jsonParams.isNull("shipping_surname") }
        assertTrue { jsonParams.isNull("shipping_phone") }
        assertTrue { jsonParams.isNull("shipping_line1") }
        assertTrue { jsonParams.isNull("shipping_line2") }
        assertTrue { jsonParams.isNull("shipping_line3") }
        assertTrue { jsonParams.isNull("shipping_city") }
        assertTrue { jsonParams.isNull("shipping_state") }
        assertTrue { jsonParams.isNull("shipping_postal_code") }
        assertTrue { jsonParams.isNull("shipping_country_code") }
        assertTrue { jsonParams.isNull("shipping_method_indicator") }
        assertTrue { jsonParams.isNull("product_code") }
        assertTrue { jsonParams.isNull("delivery_timeframe") }
        assertTrue { jsonParams.isNull("delivery_email") }
        assertTrue { jsonParams.isNull("reorder_indicator") }
        assertTrue { jsonParams.isNull("preorder_indicator") }
        assertTrue { jsonParams.isNull("preorder_date") }
        assertTrue { jsonParams.isNull("gift_card_amount") }
        assertTrue { jsonParams.isNull("gift_card_currency_code") }
        assertTrue { jsonParams.isNull("gift_card_count") }
        assertTrue { jsonParams.isNull("account_age_indicator") }
        assertTrue { jsonParams.isNull("account_create_date") }
        assertTrue { jsonParams.isNull("account_change_indicator") }
        assertTrue { jsonParams.isNull("account_change_date") }
        assertTrue { jsonParams.isNull("account_pwd_change_indicator") }
        assertTrue { jsonParams.isNull("account_pwd_change_date") }
        assertTrue { jsonParams.isNull("shipping_address_usage_indicator") }
        assertTrue { jsonParams.isNull("shipping_address_usage_date") }
        assertTrue { jsonParams.isNull("transaction_count_day") }
        assertTrue { jsonParams.isNull("transaction_count_year") }
        assertTrue { jsonParams.isNull("add_card_attempts") }
        assertTrue { jsonParams.isNull("account_purchases") }
        assertTrue { jsonParams.isNull("fraud_activity") }
        assertTrue { jsonParams.isNull("shipping_name_indicator") }
        assertTrue { jsonParams.isNull("payment_account_indicator") }
        assertTrue { jsonParams.isNull("payment_account_age") }
        assertTrue { jsonParams.isNull("address_match") }
        assertTrue { jsonParams.isNull("account_id") }
        assertTrue { jsonParams.isNull("ip_address") }
        assertTrue { jsonParams.isNull("order_description") }
        assertTrue { jsonParams.isNull("tax_amount") }
        assertTrue { jsonParams.isNull("user_agent") }
        assertTrue { jsonParams.isNull("authentication_indicator") }
        assertTrue { jsonParams.isNull("installment") }
        assertTrue { jsonParams.isNull("purchase_date") }
        assertTrue { jsonParams.isNull("recurring_end") }
        assertTrue { jsonParams.isNull("recurring_frequency") }
        assertTrue { jsonParams.isNull("sdk_max_timeout") }
        assertTrue { jsonParams.isNull("work_phone_number") }
    }
}
