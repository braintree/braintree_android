package com.braintreepayments.api.paymentactions

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.io.IOException

@Suppress("MaxLineLength")
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentActionsServiceUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    private val paymentMethodVariables = JSONObject().apply {
        put(
            "paymentMethodDetails",
            JSONObject().apply {
                put(
                    "creditCard",
                    JSONObject().apply {
                        put("number", "4111111111111111")
                        put("expirationMonth", "12")
                        put("expirationYear", "2028")
                    },
                )
            },
        )
    }

    private fun mockPaymentMethod(
        variables: JSONObject = paymentMethodVariables,
        selectionSet: String = "id\nstatus",
    ) = mockk<PaymentActionPaymentMethod> {
        every { toGraphQLVariables() } returns variables
        every { paymentActionSelectionSet() } returns selectionSet
    }

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

            assertTrue(result is PaymentActionResult.Success)
            assertEquals("pa123", (result as PaymentActionResult.Success).paymentAction.id)
            assertEquals(PaymentActionStatus.SUCCEEDED, result.paymentAction.status)
        }

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

            val success = result as PaymentActionResult.Success
            assertEquals(PaymentActionStatus.REQUIRES_CAPTURE, success.paymentAction.status)
        }

    @Test
    fun `when responseBody status is ready_for_confirmation, Success is returned with READY_FOR_CONFIRMATION status`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "setPaymentActionPaymentMethod": {
                            "paymentAction": {
                                "id": "pa123",
                                "status": "ready_for_confirmation"
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

            assertTrue(result is PaymentActionResult.Success)
            assertEquals(
                PaymentActionStatus.READY_FOR_CONFIRMATION,
                (result as PaymentActionResult.Success).paymentAction.status
            )
        }

    @Test
    fun `when responseBody status is canceled, Success is returned with CANCELED status`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "setPaymentActionPaymentMethod": {
                            "paymentAction": {
                                "id": "pa123",
                                "status": "canceled"
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

            assertTrue(result is PaymentActionResult.Success)
            assertEquals(
                PaymentActionStatus.CANCELED,
                (result as PaymentActionResult.Success).paymentAction.status
            )
        }

    @Test
    fun `when responseBody status is expired, Success is returned with EXPIRED status`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "setPaymentActionPaymentMethod": {
                            "paymentAction": {
                                "id": "pa123",
                                "status": "expired"
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

            assertTrue(result is PaymentActionResult.Success)
            assertEquals(
                PaymentActionStatus.EXPIRED,
                (result as PaymentActionResult.Success).paymentAction.status
            )
        }

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

            assertTrue(result is PaymentActionResult.Success)
            assertEquals(
                PaymentActionStatus.UNKNOWN,
                (result as PaymentActionResult.Success).paymentAction.status
            )
        }

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

            assertTrue(result is PaymentActionResult.Failure)
            assertEquals(error, (result as PaymentActionResult.Failure).error)
        }

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

            assertTrue(result is PaymentActionResult.Failure)
            assertTrue((result as PaymentActionResult.Failure).error is BraintreeException)
        }

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

            assertTrue(result is PaymentActionResult.Success)
            assertEquals("pa123", (result as PaymentActionResult.Success).paymentAction.id)
        }

    @Test
    fun `when responseBody is malformed JSON, Failure is returned with a JSONException`() =
        runTest(testDispatcher) {
            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse("not valid json")
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            assertTrue(result is PaymentActionResult.Failure)
            assertTrue((result as PaymentActionResult.Failure).error is JSONException)
        }

    @Test
    fun `when responseBody is missing the data key, Failure is returned with a JSONException`() =
        runTest(testDispatcher) {
            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse("{}")
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            assertTrue(result is PaymentActionResult.Failure)
            assertTrue((result as PaymentActionResult.Failure).error is JSONException)
        }

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

            assertTrue(result is PaymentActionResult.Failure)
            assertTrue((result as PaymentActionResult.Failure).error is JSONException)
        }

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

            assertTrue(result is PaymentActionResult.Failure)
            assertTrue((result as PaymentActionResult.Failure).error is JSONException)
        }

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

            assertTrue(result is PaymentActionResult.Failure)
            assertTrue((result as PaymentActionResult.Failure).error is JSONException)
        }

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

            assertTrue(result is PaymentActionResult.Failure)
            assertTrue((result as PaymentActionResult.Failure).error is JSONException)
        }

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

            assertTrue(result is PaymentActionResult.Failure)
            assertTrue((result as PaymentActionResult.Failure).error is JSONException)
        }

    private fun String.normalizeWhitespace(): String = trim().replace(Regex("\\s+"), " ")
}
