package com.braintreepayments.api.localpayment

import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.PostalAddress
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4ClassRunner::class)
class LocalPaymentClientTest {
    private lateinit var countDownLatch: CountDownLatch
    private lateinit var braintreeClient: BraintreeClient

    @Before
    fun setUp() {
        countDownLatch = CountDownLatch(1)
        braintreeClient = BraintreeClient(
            ApplicationProvider.getApplicationContext(),
            "sandbox_f252zhq7_hh4cpc39zq4rgjcg"
        )
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_callsBack_withApprovalUrl_andPaymentId() {
        val address = PostalAddress()
        address.streetAddress = "836486 of 22321 Park Lake"
        address.countryCodeAlpha2 = "NL"
        address.locality = "Den Haag"
        address.postalCode = "2585 GJ"

        val request = LocalPaymentRequest(true)
        request.paymentType = "ideal"
        request.amount = "1.10"
        request.address = address
        request.phone = "639847934"
        request.email = "jon@getbraintree.com"
        request.givenName = "Jon"
        request.surname = "Doe"
        request.isShippingAddressRequired = true
        request.currencyCode = "EUR"

        val sut = LocalPaymentClient(braintreeClient!!)
        sut.createPaymentAuthRequest(request) { localPaymentAuthRequest: LocalPaymentAuthRequest ->
            assertTrue(localPaymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
            assertNotNull((localPaymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams.approvalUrl)
            assertNotNull(localPaymentAuthRequest.requestParams.paymentId)
            countDownLatch!!.countDown()
        }

        countDownLatch!!.await()
    }
}
