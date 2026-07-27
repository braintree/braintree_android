package com.braintreepayments.api.core.paymentactions

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Suppress("MaxLineLength")
class PaymentActionsServiceUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    private val paymentMethodVariables = JSONObject().apply {
        put("paymentMethodId", "pm456")
    }

    private fun mockPaymentMethod(
        variables: JSONObject = paymentMethodVariables,
        selectionSet: String = "id\nstatus",
    ) = mockk<PaymentActionPaymentMethod> {
        every { toGraphQLVariables() } returns variables
        every { paymentActionSelectionSet() } returns selectionSet
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when setPaymentActionPaymentMethod is called, the correct GraphQL request body is sent`() =
        runTest(testDispatcher) {
            val braintreeClient = mockk<BraintreeClient>(relaxed = true)
            val paymentMethod = mockPaymentMethod()
            val service = PaymentActionsService(braintreeClient)

            val expectedQuery = """
                mutation SetPaymentActionPaymentMethod(${'$'}input: SetPaymentActionPaymentMethodInput!) {
                    setPaymentActionPaymentMethod(input: ${'$'}input) {
                        paymentAction {
                            id status
                        }
                    }
                }
            """
            val expectedVariables = JSONObject().apply {
                put(
                    "input",
                    JSONObject().apply {
                        put("paymentMethod", paymentMethodVariables)
                    }
                )
            }

            service.setPaymentActionPaymentMethod(paymentMethod)
            advanceUntilIdle()

            coVerify {
                braintreeClient.sendGraphQLPOST(withArg { actualRequestBody ->
                    assertEquals(
                        expectedQuery.normalizeWhitespace(),
                        actualRequestBody.getString("query").normalizeWhitespace()
                    )
                    JSONAssert.assertEquals(
                        expectedVariables,
                        actualRequestBody.getJSONObject("variables"),
                        false
                    )
                })
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when setPaymentActionPaymentMethod is called and a valid responseBody is returned, Success is returned`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "setPaymentActionPaymentMethod": {
                            "paymentAction": {
                                "id": "pa123",
                                "status": "succeeded"
                            }
                        }
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val success = assertIs<PaymentActionResult.Success>(result)
            assertEquals("pa123", success.paymentAction.id)
            assertEquals(PaymentActionStatus.SUCCEEDED, success.paymentAction.status)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when responseBody status is requires_capture, Success is returned with REQUIRES_CAPTURE status`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "setPaymentActionPaymentMethod": {
                            "paymentAction": {
                                "id": "pa123",
                                "status": "requires_capture"
                            }
                        }
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val success = assertIs<PaymentActionResult.Success>(result)
            assertEquals(PaymentActionStatus.REQUIRES_CAPTURE, success.paymentAction.status)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when responseBody status is requires_payment_method, Success is returned with REQUIRES_PAYMENT_METHOD status`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "setPaymentActionPaymentMethod": {
                            "paymentAction": {
                                "id": "pa123",
                                "status": "requires_payment_method"
                            }
                        }
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val success = assertIs<PaymentActionResult.Success>(result)
            assertEquals(PaymentActionStatus.REQUIRES_PAYMENT_METHOD, success.paymentAction.status)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when responseBody status is an unrecognized value, Success is returned with UNKNOWN status`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "setPaymentActionPaymentMethod": {
                            "paymentAction": {
                                "id": "pa123",
                                "status": "processing"
                            }
                        }
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val success = assertIs<PaymentActionResult.Success>(result)
            assertEquals(PaymentActionStatus.UNKNOWN, success.paymentAction.status)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when BraintreeClient throws IOException, Failure is returned with the same exception`() =
        runTest(testDispatcher) {
            val error = IOException("Network error")

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostErrorResponse(error)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val failure = assertIs<PaymentActionResult.Failure>(result)
            assertEquals(error, failure.error)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when responseBody has a non-empty errors array, Failure is returned with a BraintreeException`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "errors": [
                        { "message": "payment action not found" }
                    ]
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val failure = assertIs<PaymentActionResult.Failure>(result)
            assertIs<BraintreeException>(failure.error)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when responseBody has an empty errors array, Success is parsed from data as normal`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "errors": [],
                    "data": {
                        "setPaymentActionPaymentMethod": {
                            "paymentAction": {
                                "id": "pa123",
                                "status": "succeeded"
                            }
                        }
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val success = assertIs<PaymentActionResult.Success>(result)
            assertEquals("pa123", success.paymentAction.id)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when responseBody is malformed JSON, Failure is returned with a JSONException`() =
        runTest(testDispatcher) {
            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse("not valid json")
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val failure = assertIs<PaymentActionResult.Failure>(result)
            assertIs<JSONException>(failure.error)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when responseBody is missing the data key, Failure is returned with a JSONException`() =
        runTest(testDispatcher) {
            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse("{}")
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val failure = assertIs<PaymentActionResult.Failure>(result)
            assertIs<JSONException>(failure.error)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when responseBody's data value is not an object, Failure is returned with a JSONException`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": "not an object"
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val failure = assertIs<PaymentActionResult.Failure>(result)
            assertIs<JSONException>(failure.error)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when responseBody's errors value is not an array, Failure is returned with a JSONException`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "errors": "not an array"
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val failure = assertIs<PaymentActionResult.Failure>(result)
            assertIs<JSONException>(failure.error)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when responseBody is missing the paymentAction key, Failure is returned with a JSONException`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "setPaymentActionPaymentMethod": {}
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val failure = assertIs<PaymentActionResult.Failure>(result)
            assertIs<JSONException>(failure.error)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when responseBody is missing the id field, Failure is returned with a JSONException`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "setPaymentActionPaymentMethod": {
                            "paymentAction": {
                                "status": "succeeded"
                            }
                        }
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val failure = assertIs<PaymentActionResult.Failure>(result)
            assertIs<JSONException>(failure.error)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when responseBody is missing the status field, Failure is returned with a JSONException`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "setPaymentActionPaymentMethod": {
                            "paymentAction": {
                                "id": "pa123"
                            }
                        }
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            val failure = assertIs<PaymentActionResult.Failure>(result)
            assertIs<JSONException>(failure.error)
        }

    private fun String.normalizeWhitespace(): String = trim().replace(Regex("\\s+"), " ")
}
