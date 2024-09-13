package com.braintreepayments.api.shopperinsights

import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ConfigurationCallback
import com.braintreepayments.api.sharedutils.HttpResponseCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class EligiblePaymentsApiUnitTest {

    private lateinit var sut: EligiblePaymentsApi
    private lateinit var callback: EligiblePaymentsCallback
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var analyticsParamRepository: AnalyticsParamRepository
    private val configuration: Configuration = createMockConfiguration()

    @Before
    fun setup() {
        callback = mockk(relaxed = true)
        braintreeClient = mockk(relaxed = true)
        analyticsParamRepository = mockk(relaxed = true)
        setupBraintreeClientToReturnConfiguration()
        sut = EligiblePaymentsApi(braintreeClient, analyticsParamRepository)
    }

    @Test
    fun `when environment is production, braintreeClient sendPOST is called with the correct url`() {
        val expectedUrl = "https://api.paypal.com/v2/payments/find-eligible-methods"
        every { configuration.environment } returns "production"

        sut.execute(createEmptyRequest(), callback)

        verify { braintreeClient.sendPOST(expectedUrl, any(), any(), any()) }
    }

    @Test
    fun `when environment is sandbox, braintreeClient sendPOST is called with the correct url`() {
        val expectedUrl = "https://api.sandbox.paypal.com/v2/payments/find-eligible-methods"
        every { configuration.environment } returns "sandbox"

        sut.execute(createEmptyRequest(), callback)

        verify { braintreeClient.sendPOST(expectedUrl, any(), any(), any()) }
    }

    @Test
    fun `PAYPAL_CLIENT_METADATA_ID header is sent to the braintreeClient post call`() {
        val sessionId = "session-id-value"
        every { analyticsParamRepository.sessionId } returns sessionId

        sut.execute(mockk(relaxed = true), mockk())

        verify {
            braintreeClient.sendPOST(any(), any(), withArg { headers ->
                assertEquals(headers["PayPal-Client-Metadata-Id"], sessionId)
            }, any())
        }
    }

    @Test
    fun `when sendPost is called and an error occurs, callback onResult is invoked with the error`() {
        val error = Exception("error")

        mockBraintreeClientToSendPOSTWithError(error)

        sut.execute(createEmptyRequest(), callback)

        verify {
            callback.onResult(result = null, error = error)
        }
    }

    @Test
    fun `when sendPost is called, callback onResult is invoked with a result`() {

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
        val callbackSlot = slot<ConfigurationCallback>()
        every {
            braintreeClient.getConfiguration(capture(callbackSlot))
        } answers {
            val callback = callbackSlot.captured
            callback.onResult(configuration, error = null)
        }
    }

    private fun createMockConfiguration(): Configuration {
        return mockk(relaxed = true) {
            every { environment } answers { "sandbox" }
        }
    }

    private fun mockBraintreeClientToSendPOSTWithError(error: Exception) {
        val callbackSlot = slot<HttpResponseCallback>()
        every {
            braintreeClient.sendPOST(any(), any(), any(), capture(callbackSlot))
        } answers {
            val callback = callbackSlot.captured
            callback.onResult(null, error)
        }
    }

    private fun mockBraintreeClientToSendPOSTWithResponse(responseBody: String) {
        val callbackSlot = slot<HttpResponseCallback>()
        every {
            braintreeClient.sendPOST(any(), any(), any(), capture(callbackSlot))
        } answers {
            val callback = callbackSlot.captured
            callback.onResult(responseBody, null)
        }
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
