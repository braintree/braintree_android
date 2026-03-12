package com.braintreepayments.api.shopperinsights

import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ExperimentalBetaApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class EligiblePaymentsApiUnitTest {

    private lateinit var sut: EligiblePaymentsApi
    private lateinit var callback: EligiblePaymentsCallback
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var analyticsParamRepository: AnalyticsParamRepository
    private val configuration: Configuration = createMockConfiguration()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        callback = mockk(relaxed = true)
        braintreeClient = mockk(relaxed = true)
        analyticsParamRepository = mockk(relaxed = true)
        setupBraintreeClientToReturnConfiguration()
        val testScope = TestScope(testDispatcher)
        sut = EligiblePaymentsApi(
            braintreeClient,
            analyticsParamRepository,
            dispatcher = testDispatcher,
            coroutineScope = testScope
        )
    }

    @Test
    fun `when environment is production, braintreeClient sendPOST is called with the correct url`() =
        runTest(testDispatcher) {
        val expectedUrl = "https://api.paypal.com/v2/payments/find-eligible-methods"
        every { configuration.environment } returns "production"
        coEvery { braintreeClient.sendPOST(url = any(), data = any(), additionalHeaders = any()) } returns "{}"

        sut.execute(createEmptyRequest(), callback)
        advanceUntilIdle()
        coVerify { braintreeClient.sendPOST(url = expectedUrl, data = any(), additionalHeaders = any()) }
        }

    @Test
    fun `when environment is sandbox, braintreeClient sendPOST is called with the correct url`() =
        runTest(testDispatcher) {
        val expectedUrl = "https://api.sandbox.paypal.com/v2/payments/find-eligible-methods"
        every { configuration.environment } returns "sandbox"
        coEvery { braintreeClient.sendPOST(url = any(), data = any(), additionalHeaders = any()) } returns "{}"

        sut.execute(createEmptyRequest(), callback)
        advanceUntilIdle()
        coVerify { braintreeClient.sendPOST(url = expectedUrl, data = any(), additionalHeaders = any()) }
    }

    @Test
    fun `PAYPAL_CLIENT_METADATA_ID header is sent to the braintreeClient post call`() = runTest(testDispatcher) {
        val sessionId = "session-id-value"
        every { analyticsParamRepository.sessionId } returns sessionId

        val headersSlot = slot<Map<String, String>>()
        coEvery {
            braintreeClient.sendPOST(url = any(), data = any(), additionalHeaders = capture(headersSlot))
        } returns "{}"
        sut.execute(mockk(relaxed = true), mockk())
        advanceUntilIdle()
        assertEquals(sessionId, headersSlot.captured["PayPal-Client-Metadata-Id"])
    }

    @Test
    fun `when sendPost is called and an error occurs, callback onResult is invoked with the error`() =
        runTest(testDispatcher) {
        val error = IOException("error")

        mockBraintreeClientToSendPOSTWithError(error)

        sut.execute(createEmptyRequest(), callback)
        advanceUntilIdle()
        verify {
            callback.onResult(result = null, error = error)
        }
    }

    @Test
    fun `when sendPost is called, callback onResult is invoked with a result`() = runTest(testDispatcher) {

        val responseBody = """
            {
                "eligible_methods": {
                    "paypal": {
                        "can_be_vaulted": true,
                        "eligible_in_paypal_network": false,
                        "recommended": true,
                        "recommended_priority": 1
                    }
                }
            }
        """.trimIndent()

        mockBraintreeClientToSendPOSTWithResponse(responseBody)

        sut.execute(createEmptyRequest(), callback)
        advanceUntilIdle()
        verify {
            callback.onResult(
                result = EligiblePaymentsApiResult(
                    EligiblePaymentMethods(
                        paypal = EligiblePaymentMethodDetails(
                            canBeVaulted = true,
                            eligibleInPayPalNetwork = false,
                            recommended = true,
                            recommendedPriority = 1
                        ),
                        venmo = null
                    )
                ), error = null
            )
        }
    }

    private fun setupBraintreeClientToReturnConfiguration() {
        coEvery {
            braintreeClient.getConfiguration()
        } returns configuration
    }

    private fun createMockConfiguration(): Configuration {
        return mockk(relaxed = true) {
            every { environment } answers { "sandbox" }
        }
    }

    private fun mockBraintreeClientToSendPOSTWithError(error: IOException) {
        coEvery {
            braintreeClient.sendPOST(url = any(), data = any(), additionalHeaders = any())
        } throws error
    }

    private fun mockBraintreeClientToSendPOSTWithResponse(responseBody: String) {
        coEvery {
            braintreeClient.sendPOST(url = any(), data = any(), additionalHeaders = any())
        } returns responseBody
    }

    @OptIn(ExperimentalBetaApi::class)
    private fun createEmptyRequest(): EligiblePaymentsApiRequest {
        return EligiblePaymentsApiRequest(
            ShopperInsightsRequest(
                "",
                ShopperInsightsBuyerPhone("", "")
            ),
            "",
            "",
            true,
            "",
            emptyList()
        )
    }
}
