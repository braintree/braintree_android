package com.braintreepayments.api.sepadirectdebit

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.PostalAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class SEPADirectDebitRequestTest {

    @Test
    fun defaultValues_areCorrect() {
        val request = SEPADirectDebitRequest()

        assertNull(request.accountHolderName)
        assertNull(request.iban)
        assertNull(request.customerId)
        assertEquals(SEPADirectDebitMandateType.ONE_OFF, request.mandateType)
        assertNull(request.billingAddress)
        assertNull(request.merchantAccountId)
        assertNull(request.locale)
    }

    @Test
    fun allFields_canBeSet() {
        val billingAddress = PostalAddress()
        billingAddress.streetAddress = "Kantstraße 70"
        billingAddress.extendedAddress = "Apt 1"
        billingAddress.locality = "Freistaat Sachsen"
        billingAddress.region = "Sachsen"
        billingAddress.postalCode = "01069"
        billingAddress.countryCodeAlpha2 = "DE"

        val request = SEPADirectDebitRequest(
            accountHolderName = "John Doe",
            iban = "DE89370400440532013000",
            customerId = "customer-123",
            mandateType = SEPADirectDebitMandateType.RECURRENT,
            billingAddress = billingAddress,
            merchantAccountId = "EUR-sepa-direct-debit",
            locale = "en-US"
        )

        assertEquals("John Doe", request.accountHolderName)
        assertEquals("DE89370400440532013000", request.iban)
        assertEquals("customer-123", request.customerId)
        assertEquals(SEPADirectDebitMandateType.RECURRENT, request.mandateType)
        assertEquals("Kantstraße 70", request.billingAddress?.streetAddress)
        assertEquals("Apt 1", request.billingAddress?.extendedAddress)
        assertEquals("Freistaat Sachsen", request.billingAddress?.locality)
        assertEquals("Sachsen", request.billingAddress?.region)
        assertEquals("01069", request.billingAddress?.postalCode)
        assertEquals("DE", request.billingAddress?.countryCodeAlpha2)
        assertEquals("EUR-sepa-direct-debit", request.merchantAccountId)
        assertEquals("en-US", request.locale)
    }
}
