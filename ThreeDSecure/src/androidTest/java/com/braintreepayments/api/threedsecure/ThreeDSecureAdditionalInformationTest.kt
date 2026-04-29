package com.braintreepayments.api.threedsecure

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.parcelize.parcelableCreator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class ThreeDSecureAdditionalInformationTest {

    @Suppress("LongMethod")
    @Test
    fun parcels_withAllFieldsPopulated() {
        val shippingAddress = ThreeDSecurePostalAddress(
            givenName = "Joe",
            surname = "Guy",
            streetAddress = "555 Smith St.",
            extendedAddress = "Unit 1",
            line3 = "Building A",
            locality = "Oakland",
            region = "CA",
            postalCode = "12345",
            countryCodeAlpha2 = "US",
            phoneNumber = "5551234567"
        )

        val original = ThreeDSecureAdditionalInformation(
            shippingAddress = shippingAddress,
            shippingMethodIndicator = "01",
            productCode = "AIR",
            deliveryTimeframe = "02",
            deliveryEmail = "test@example.com",
            reorderIndicator = "01",
            preorderIndicator = "02",
            preorderDate = "20250101",
            giftCardAmount = "100",
            giftCardCurrencyCode = "USD",
            giftCardCount = "2",
            accountAgeIndicator = "05",
            accountCreateDate = "20200101",
            accountChangeIndicator = "04",
            accountChangeDate = "20240601",
            accountPwdChangeIndicator = "03",
            accountPwdChangeDate = "20240501",
            shippingAddressUsageIndicator = "04",
            shippingAddressUsageDate = "20230101",
            transactionCountDay = "3",
            transactionCountYear = "50",
            addCardAttempts = "1",
            accountPurchases = "20",
            fraudActivity = "01",
            shippingNameIndicator = "01",
            paymentAccountIndicator = "05",
            paymentAccountAge = "20190601",
            addressMatch = "Y",
            accountId = "user-123",
            ipAddress = "192.168.1.1",
            orderDescription = "Test order",
            taxAmount = "1000",
            userAgent = "Mozilla/5.0",
            authenticationIndicator = "02",
            installment = "3",
            purchaseDate = "20250101120000",
            recurringEnd = "20261231",
            recurringFrequency = "28",
            sdkMaxTimeout = "05",
            workPhoneNumber = "5559876543"
        )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<ThreeDSecureAdditionalInformation>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals(original.shippingAddress?.givenName, restored.shippingAddress?.givenName)
        assertEquals(original.shippingAddress?.surname, restored.shippingAddress?.surname)
        assertEquals(original.shippingAddress?.streetAddress, restored.shippingAddress?.streetAddress)
        assertEquals(original.shippingAddress?.locality, restored.shippingAddress?.locality)
        assertEquals(original.shippingAddress?.postalCode, restored.shippingAddress?.postalCode)
        assertEquals(original.shippingMethodIndicator, restored.shippingMethodIndicator)
        assertEquals(original.productCode, restored.productCode)
        assertEquals(original.deliveryTimeframe, restored.deliveryTimeframe)
        assertEquals(original.deliveryEmail, restored.deliveryEmail)
        assertEquals(original.reorderIndicator, restored.reorderIndicator)
        assertEquals(original.preorderIndicator, restored.preorderIndicator)
        assertEquals(original.preorderDate, restored.preorderDate)
        assertEquals(original.giftCardAmount, restored.giftCardAmount)
        assertEquals(original.giftCardCurrencyCode, restored.giftCardCurrencyCode)
        assertEquals(original.giftCardCount, restored.giftCardCount)
        assertEquals(original.accountAgeIndicator, restored.accountAgeIndicator)
        assertEquals(original.accountCreateDate, restored.accountCreateDate)
        assertEquals(original.accountChangeIndicator, restored.accountChangeIndicator)
        assertEquals(original.accountChangeDate, restored.accountChangeDate)
        assertEquals(original.accountPwdChangeIndicator, restored.accountPwdChangeIndicator)
        assertEquals(original.accountPwdChangeDate, restored.accountPwdChangeDate)
        assertEquals(original.shippingAddressUsageIndicator, restored.shippingAddressUsageIndicator)
        assertEquals(original.shippingAddressUsageDate, restored.shippingAddressUsageDate)
        assertEquals(original.transactionCountDay, restored.transactionCountDay)
        assertEquals(original.transactionCountYear, restored.transactionCountYear)
        assertEquals(original.addCardAttempts, restored.addCardAttempts)
        assertEquals(original.accountPurchases, restored.accountPurchases)
        assertEquals(original.fraudActivity, restored.fraudActivity)
        assertEquals(original.shippingNameIndicator, restored.shippingNameIndicator)
        assertEquals(original.paymentAccountIndicator, restored.paymentAccountIndicator)
        assertEquals(original.paymentAccountAge, restored.paymentAccountAge)
        assertEquals(original.addressMatch, restored.addressMatch)
        assertEquals(original.accountId, restored.accountId)
        assertEquals(original.ipAddress, restored.ipAddress)
        assertEquals(original.orderDescription, restored.orderDescription)
        assertEquals(original.taxAmount, restored.taxAmount)
        assertEquals(original.userAgent, restored.userAgent)
        assertEquals(original.authenticationIndicator, restored.authenticationIndicator)
        assertEquals(original.installment, restored.installment)
        assertEquals(original.purchaseDate, restored.purchaseDate)
        assertEquals(original.recurringEnd, restored.recurringEnd)
        assertEquals(original.recurringFrequency, restored.recurringFrequency)
        assertEquals(original.sdkMaxTimeout, restored.sdkMaxTimeout)
        assertEquals(original.workPhoneNumber, restored.workPhoneNumber)
    }

    @Test
    fun parcels_withDefaultValues() {
        val original = ThreeDSecureAdditionalInformation()

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<ThreeDSecureAdditionalInformation>().createFromParcel(parcel)
        parcel.recycle()

        assertNull(restored.shippingAddress)
        assertNull(restored.shippingMethodIndicator)
        assertNull(restored.productCode)
        assertNull(restored.deliveryTimeframe)
        assertNull(restored.deliveryEmail)
        assertNull(restored.accountId)
        assertNull(restored.ipAddress)
        assertNull(restored.workPhoneNumber)
    }

    @Test
    fun parcels_withNestedShippingAddress() {
        val shippingAddress = ThreeDSecurePostalAddress(
            givenName = "Jane",
            surname = "Doe",
            streetAddress = "123 Main St",
            extendedAddress = "Suite 200",
            line3 = "Floor 3",
            locality = "Chicago",
            region = "IL",
            postalCode = "60601",
            countryCodeAlpha2 = "US",
            phoneNumber = "3125551234"
        )
        val original = ThreeDSecureAdditionalInformation(shippingAddress = shippingAddress)

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<ThreeDSecureAdditionalInformation>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals("Jane", restored.shippingAddress?.givenName)
        assertEquals("Doe", restored.shippingAddress?.surname)
        assertEquals("123 Main St", restored.shippingAddress?.streetAddress)
        assertEquals("Suite 200", restored.shippingAddress?.extendedAddress)
        assertEquals("Floor 3", restored.shippingAddress?.line3)
        assertEquals("Chicago", restored.shippingAddress?.locality)
        assertEquals("IL", restored.shippingAddress?.region)
        assertEquals("60601", restored.shippingAddress?.postalCode)
        assertEquals("US", restored.shippingAddress?.countryCodeAlpha2)
        assertEquals("3125551234", restored.shippingAddress?.phoneNumber)
    }

    @Test
    fun toJson_withAllFieldsPopulated() {
        val shippingAddress = ThreeDSecurePostalAddress(
            givenName = "Joe",
            surname = "Guy",
            streetAddress = "555 Smith St.",
            locality = "Oakland",
            region = "CA",
            postalCode = "12345",
            countryCodeAlpha2 = "US",
            phoneNumber = "5551234567"
        )

        val additionalInfo = ThreeDSecureAdditionalInformation(
            shippingAddress = shippingAddress,
            shippingMethodIndicator = "01",
            productCode = "AIR",
            deliveryTimeframe = "02",
            deliveryEmail = "test@example.com",
            reorderIndicator = "01",
            preorderIndicator = "02",
            preorderDate = "20250101",
            giftCardAmount = "100",
            giftCardCurrencyCode = "USD",
            giftCardCount = "2",
            accountAgeIndicator = "05",
            accountCreateDate = "20200101",
            ipAddress = "192.168.1.1",
            orderDescription = "Test order",
            taxAmount = "1000",
            workPhoneNumber = "5559876543"
        )

        val json = additionalInfo.toJson()

        assertEquals("Joe", json.getString("shipping_given_name"))
        assertEquals("Guy", json.getString("shipping_surname"))
        assertEquals("555 Smith St.", json.getString("shipping_line1"))
        assertEquals("Oakland", json.getString("shipping_city"))
        assertEquals("CA", json.getString("shipping_state"))
        assertEquals("12345", json.getString("shipping_postal_code"))
        assertEquals("US", json.getString("shipping_country_code"))
        assertEquals("5551234567", json.getString("shipping_phone"))
        assertEquals("01", json.getString("shipping_method_indicator"))
        assertEquals("AIR", json.getString("product_code"))
        assertEquals("02", json.getString("delivery_timeframe"))
        assertEquals("test@example.com", json.getString("delivery_email"))
        assertEquals("01", json.getString("reorder_indicator"))
        assertEquals("02", json.getString("preorder_indicator"))
        assertEquals("20250101", json.getString("preorder_date"))
        assertEquals("100", json.getString("gift_card_amount"))
        assertEquals("USD", json.getString("gift_card_currency_code"))
        assertEquals("2", json.getString("gift_card_count"))
        assertEquals("05", json.getString("account_age_indicator"))
        assertEquals("20200101", json.getString("account_create_date"))
        assertEquals("192.168.1.1", json.getString("ip_address"))
        assertEquals("Test order", json.getString("order_description"))
        assertEquals("1000", json.getString("tax_amount"))
        assertEquals("5559876543", json.getString("work_phone_number"))
    }

    @Test
    fun toJson_withEmptyFields() {
        val additionalInfo = ThreeDSecureAdditionalInformation()

        val json = additionalInfo.toJson()

        assertEquals(0, json.length())
    }
}
