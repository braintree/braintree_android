package com.braintreepayments.api.localpayment

import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.PostalAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class LocalPaymentApiTest {

    private lateinit var braintreeClient: BraintreeClient
    private lateinit var sut: LocalPaymentApi

    @Before
    fun setUp() {
        braintreeClient = BraintreeClient(
            ApplicationProvider.getApplicationContext(),
            "sandbox_f252zhq7_hh4cpc39zq4rgjcg"
        )
        sut = LocalPaymentApi(braintreeClient)
    }

    @Test(timeout = 10000)
    fun createPaymentMethod_withValidRequest_returnsParams() {
        runBlocking {
            val address = PostalAddress()
            address.streetAddress = "836486 of 22321 Park Lake"
            address.countryCodeAlpha2 = "NL"
            address.locality = "Den Haag"
            address.postalCode = "2585 GJ"

            val request = LocalPaymentRequest(
                hasUserLocationConsent = true,
                address = address,
                amount = "1.10",
                currencyCode = "EUR",
                email = "jon@getbraintree.com",
                givenName = "Jon",
                merchantAccountId = "altpay_eur",
                phone = "639847934",
                paymentType = "ideal",
                isShippingAddressRequired = true,
                paymentTypeCountryCode = "NL"
            )

            val result = sut.createPaymentMethod(request)

            assertTrue(result.approvalUrl.isNotEmpty())
            assertTrue(result.paymentId.isNotEmpty())
        }
    }

    @Test(timeout = 10000)
    fun createPaymentMethod_andTokenize_returnsNonce() {
        runBlocking {
            val address = PostalAddress()
            address.streetAddress = "836486 of 22321 Park Lake"
            address.countryCodeAlpha2 = "NL"
            address.locality = "Den Haag"
            address.postalCode = "2585 GJ"

            val request = LocalPaymentRequest(
                hasUserLocationConsent = true,
                address = address,
                amount = "1.10",
                currencyCode = "EUR",
                email = "jon@getbraintree.com",
                givenName = "Jon",
                merchantAccountId = "altpay_eur",
                phone = "639847934",
                paymentType = "ideal",
                isShippingAddressRequired = true,
                paymentTypeCountryCode = "NL"
            )

            val createResult = sut.createPaymentMethod(request)
            assertNotNull(createResult.paymentId)

            val webUrl =
                "sample-scheme://local-payment-success?paymentToken=${createResult.paymentId}"
            val nonce = sut.tokenize("altpay_eur", webUrl, "test-correlation-id")
            assertTrue(nonce.string.isNotEmpty())
        }
    }
}