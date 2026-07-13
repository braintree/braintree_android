package com.braintreepayments.api.paypal

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AutoLinkTokenizeUseCaseUnitTest {

    private val internalPayPalClient: PayPalInternalClient = mockk(relaxed = true)
    private lateinit var sut: AutoLinkTokenizeUseCase

    @Before
    fun setup() {
        sut = AutoLinkTokenizeUseCase(internalPayPalClient)
    }

    @Test
    fun `invoke tokenizes with correct PayPalAccount fields`() = runTest {
        val expectedNonce = mockk<PayPalAccountNonce>()
        val accountSlot = slot<PayPalAccount>()
        coEvery { internalPayPalClient.tokenize(capture(accountSlot)) } returns expectedNonce

        val session = PendingPaymentStore.PendingSession(
            baToken = "BA-TEST123",
            clientMetadataId = "metadata-id",
            merchantAccountId = "merchant-acct",
            intent = "authorize",
            paymentType = "billing-agreement"
        )

        val result = sut(session)

        assertSame(expectedNonce, result)

        val captured = accountSlot.captured
        assertEquals("metadata-id", captured.clientMetadataId)
        assertEquals("merchant-acct", captured.merchantAccountId)
        assertEquals("billing-agreement", captured.paymentType)
        assertEquals(PayPalPaymentIntent.AUTHORIZE, captured.intent)
    }

    @Test
    fun `invoke builds urlResponseData with billing_agreement_token`() = runTest {
        val expectedNonce = mockk<PayPalAccountNonce>()
        val accountSlot = slot<PayPalAccount>()
        coEvery { internalPayPalClient.tokenize(capture(accountSlot)) } returns expectedNonce

        val session = PendingPaymentStore.PendingSession(
            baToken = "BA-TOKEN-ABC",
            clientMetadataId = null,
            merchantAccountId = null,
            intent = null,
            paymentType = "billing-agreement"
        )

        sut(session)

        val urlResponseData = accountSlot.captured.urlResponseData
        assertEquals("BA-TOKEN-ABC", urlResponseData.getString("billing_agreement_token"))
        assertEquals("web", urlResponseData.getString("response_type"))
    }

    @Test
    fun `invoke with null intent sets null on PayPalAccount`() = runTest {
        val expectedNonce = mockk<PayPalAccountNonce>()
        val accountSlot = slot<PayPalAccount>()
        coEvery { internalPayPalClient.tokenize(capture(accountSlot)) } returns expectedNonce

        val session = PendingPaymentStore.PendingSession(
            baToken = "BA-123",
            clientMetadataId = null,
            merchantAccountId = null,
            intent = null,
            paymentType = "billing-agreement"
        )

        sut(session)

        assertEquals(null, accountSlot.captured.intent)
    }

    @Test(expected = Exception::class)
    fun `invoke propagates tokenization errors`() = runTest {
        coEvery {
            internalPayPalClient.tokenize(any())
        } throws Exception("BTGW error")

        val session = PendingPaymentStore.PendingSession(
            baToken = "BA-123",
            clientMetadataId = null,
            merchantAccountId = null,
            intent = null,
            paymentType = "billing-agreement"
        )

        sut(session)
    }
}
