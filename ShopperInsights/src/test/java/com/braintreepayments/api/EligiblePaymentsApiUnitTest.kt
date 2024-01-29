package com.braintreepayments.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class EligiblePaymentsApiUnitTest {

    private lateinit var sut: EligiblePaymentsApi
    private lateinit var callback: EligiblePaymentsCallback
    private lateinit var braintreeClient: BraintreeClient
    private val configuration: Configuration = createMockConfiguration()

    @Before
    fun setup() {
        callback = mockk(relaxed = true)
        braintreeClient = mockk(relaxed = true)
        setupBraintreeClientToReturnConfiguration()
        sut = EligiblePaymentsApi(braintreeClient)
    }

    @Test
    fun test_sendPOST_Error() {
        val error = Exception("error")

        mockBraintreeClientToSendPOSTWithError(error)

        sut.execute(createEmptyRequest(), callback)

        verify {
            callback.onResult(result = null, error = error)
        }
    }

    @Test
    fun test_sendPOST_Success() {

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
            callback.onResult(result = EligiblePaymentsApiResult(
                EligiblePaymentMethods(
                    paypal = EligiblePaymentMethodDetails(
                        canBeVaulted = true,
                        eligibleInPayPalNetwork = false,
                        recommended = true,
                        recommendedPriority = 1
                    ),
                    venmo = null
                )
            ), error = null)
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
            braintreeClient.sendPOST(any(), any(), capture(callbackSlot))
        } answers {
            val callback = callbackSlot.captured
            callback.onResult(null, error)
        }
    }

    private fun mockBraintreeClientToSendPOSTWithResponse(responseBody: String) {
        val callbackSlot = slot<HttpResponseCallback>()
        every {
            braintreeClient.sendPOST(any(), any(), capture(callbackSlot))
        } answers {
            val callback = callbackSlot.captured
            callback.onResult(responseBody, null)
        }
    }

    private fun createEmptyRequest(): EligiblePaymentsApiRequest {
       return EligiblePaymentsApiRequest(
            ShopperInsightsRequest(
                "",
                ShopperInsightsBuyerPhone("", "")
            ),
            "",
            "",
            "",
            true,
            "",
            emptyList()
        )
    }
}
