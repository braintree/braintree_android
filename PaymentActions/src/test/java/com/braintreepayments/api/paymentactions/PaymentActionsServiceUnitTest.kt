package com.braintreepayments.api.paymentactions

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coEvery
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
    ) = mockk<PaymentActionRequest> {
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

            assertTrue(result is PaymentActionServiceResult.Success)
            assertEquals("pa123", (result as PaymentActionServiceResult.Success).paymentAction.id)
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

            val success = result as PaymentActionServiceResult.Success
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

            assertTrue(result is PaymentActionServiceResult.Success)
            assertEquals(
                PaymentActionStatus.READY_FOR_CONFIRMATION,
                (result as PaymentActionServiceResult.Success).paymentAction.status
            )
        }

    @Test
    fun `when responseBody status is cancelled, Success is returned with CANCELLED status`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "setPaymentActionPaymentMethod": {
                            "paymentAction": {
                                "id": "pa123",
                                "status": "cancelled"
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

            assertTrue(result is PaymentActionServiceResult.Success)
            assertEquals(
                PaymentActionStatus.CANCELLED,
                (result as PaymentActionServiceResult.Success).paymentAction.status
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

            assertTrue(result is PaymentActionServiceResult.Success)
            assertEquals(
                PaymentActionStatus.EXPIRED,
                (result as PaymentActionServiceResult.Success).paymentAction.status
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
                                "status": "some_unrecognized_status"
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

            assertTrue(result is PaymentActionServiceResult.Success)
            assertEquals(
                PaymentActionStatus.UNKNOWN,
                (result as PaymentActionServiceResult.Success).paymentAction.status
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

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertEquals(error, (result as PaymentActionServiceResult.Failure).error)
        }

    @Test
    fun `when BraintreeClient throws a non-IOException, Failure is returned with the same exception`() =
        runTest(testDispatcher) {
            val error = JSONException("parse error")

            val braintreeClient = mockk<BraintreeClient>(relaxed = true)
            coEvery { braintreeClient.sendGraphQLPOST(any()) } throws error

            val service = PaymentActionsService(braintreeClient)
            val result = service.setPaymentActionPaymentMethod(mockPaymentMethod())
            advanceUntilIdle()

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertEquals(error, (result as PaymentActionServiceResult.Failure).error)
        }

    @Test
    fun `when BraintreeClient throws CancellationException, it is rethrown and not wrapped in Failure`() =
        runTest(testDispatcher) {
            val braintreeClient = mockk<BraintreeClient>(relaxed = true)
            coEvery { braintreeClient.sendGraphQLPOST(any()) } throws
                kotlin.coroutines.cancellation.CancellationException("cancelled")

            val service = PaymentActionsService(braintreeClient)
            var threwCancellation = false
            try {
                service.setPaymentActionPaymentMethod(mockPaymentMethod())
                advanceUntilIdle()
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                threwCancellation = true
            }

            assertTrue(threwCancellation)
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

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertTrue((result as PaymentActionServiceResult.Failure).error is BraintreeException)
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

            assertTrue(result is PaymentActionServiceResult.Success)
            assertEquals("pa123", (result as PaymentActionServiceResult.Success).paymentAction.id)
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

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertTrue((result as PaymentActionServiceResult.Failure).error is JSONException)
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

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertTrue((result as PaymentActionServiceResult.Failure).error is JSONException)
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

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertTrue((result as PaymentActionServiceResult.Failure).error is JSONException)
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

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertTrue((result as PaymentActionServiceResult.Failure).error is JSONException)
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

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertTrue((result as PaymentActionServiceResult.Failure).error is JSONException)
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

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertTrue((result as PaymentActionServiceResult.Failure).error is JSONException)
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

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertTrue((result as PaymentActionServiceResult.Failure).error is JSONException)
        }

    // MARK: - getPaymentAction

    @Test
    fun `when getPaymentAction is called, the correct GraphQL request body is sent`() =
        runTest(testDispatcher) {
            val braintreeClient = mockk<BraintreeClient>(relaxed = true)
            val service = PaymentActionsService(braintreeClient)

            service.getPaymentAction()
            advanceUntilIdle()

            coVerify {
                braintreeClient.sendGraphQLPOST(withArg { actualRequestBody ->
                    assertTrue(actualRequestBody.getString("query").contains("query GetPaymentAction"))
                    assertTrue(actualRequestBody.getString("query").contains("paymentAction {"))
                    assertTrue(!actualRequestBody.getString("query").contains("\$id"))
                    JSONAssert.assertEquals(JSONObject(), actualRequestBody.getJSONObject("variables"), true)
                })
            }
        }

    @Test
    fun `when getPaymentAction returns a nextAction, Success is returned with the parsed nextAction`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "paymentAction": {
                            "id": "pa123",
                            "status": "requires_customer_action",
                            "nextAction": {
                                "__typename": "PaymentActionThreeDSecureAuthentication",
                                "bin": "401288"
                            }
                        }
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.getPaymentAction()
            advanceUntilIdle()

            val success = result as PaymentActionServiceResult.Success
            assertEquals("pa123", success.paymentAction.id)
            assertEquals(
                PaymentActionNextAction.ThreeDSecureAuthentication(bin = "401288"),
                success.paymentAction.nextAction,
            )
        }

    @Test
    fun `when getPaymentAction returns a 3DS challenge nextAction, Success parses its fields`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "paymentAction": {
                            "id": "pa123",
                            "status": "requires_customer_action",
                            "nextAction": {
                                "__typename": "PaymentActionThreeDSecureChallenge",
                                "acsUrl": "https://acs.example.com",
                                "acsRenderingType": "otp",
                                "challengePayload": "payload-data"
                            }
                        }
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.getPaymentAction()
            advanceUntilIdle()

            val success = result as PaymentActionServiceResult.Success
            assertEquals(
                PaymentActionNextAction.ThreeDSecureChallenge(
                    acsUrl = "https://acs.example.com",
                    acsRenderingType = "otp",
                    challengePayload = "payload-data",
                ),
                success.paymentAction.nextAction,
            )
        }

    @Test
    fun `when getPaymentAction returns an unrecognized nextAction typename, Success returns Unrecognized`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "paymentAction": {
                            "id": "pa123",
                            "status": "requires_customer_action",
                            "nextAction": {
                                "__typename": "PaymentActionRedirectUrl"
                            }
                        }
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.getPaymentAction()
            advanceUntilIdle()

            val success = result as PaymentActionServiceResult.Success
            assertEquals(
                PaymentActionNextAction.Unrecognized(typeName = "PaymentActionRedirectUrl"),
                success.paymentAction.nextAction,
            )
        }

    @Test
    fun `when getPaymentAction returns no nextAction, Success returns a null nextAction`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "paymentAction": {
                            "id": "pa123",
                            "status": "succeeded"
                        }
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.getPaymentAction()
            advanceUntilIdle()

            val success = result as PaymentActionServiceResult.Success
            assertEquals(null, success.paymentAction.nextAction)
        }

    @Test
    fun `when getPaymentAction's nextAction is missing required fields, Failure is returned with a JSONException`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "paymentAction": {
                            "id": "pa123",
                            "status": "requires_customer_action",
                            "nextAction": {
                                "__typename": "PaymentActionThreeDSecureAuthentication"
                            }
                        }
                    }
                }
            """.trimIndent()

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.getPaymentAction()
            advanceUntilIdle()

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertTrue((result as PaymentActionServiceResult.Failure).error is JSONException)
        }

    @Test
    fun `when getPaymentAction's BraintreeClient throws an IOException, Failure is returned with the same exception`() =
        runTest(testDispatcher) {
            val error = IOException("Network error")

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostErrorResponse(error)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.getPaymentAction()
            advanceUntilIdle()

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertEquals(error, (result as PaymentActionServiceResult.Failure).error)
        }

    // MARK: - signalCustomerActionComplete

    @Test
    fun `when signalCustomerActionComplete is called with deviceDataCollection, the correct GraphQL request body is sent`() =
        runTest(testDispatcher) {
            val braintreeClient = mockk<BraintreeClient>(relaxed = true)
            val service = PaymentActionsService(braintreeClient)

            service.signalCustomerActionComplete(
                CustomerCompletedAction.DeviceDataCollection(dfReferenceId = "df-reference-id"),
            )
            advanceUntilIdle()

            val expectedVariables = JSONObject().apply {
                put(
                    "input",
                    JSONObject().apply {
                        put(
                            "customerActionComplete",
                            JSONObject().apply {
                                put("deviceDataCollection", JSONObject().apply { put("dfReferenceId", "df-reference-id") })
                            },
                        )
                    },
                )
            }

            coVerify {
                braintreeClient.sendGraphQLPOST(withArg { actualRequestBody ->
                    assertTrue(
                        actualRequestBody.getString("query").contains("mutation SignalCustomerActionComplete")
                    )
                    assertTrue(
                        !actualRequestBody.getJSONObject("variables").getJSONObject("input").has("paymentActionId")
                    )
                    JSONAssert.assertEquals(
                        expectedVariables,
                        actualRequestBody.getJSONObject("variables"),
                        false,
                    )
                })
            }
        }

    @Test
    fun `when signalCustomerActionComplete is called with challenge, only the challenge variant is encoded`() =
        runTest(testDispatcher) {
            val braintreeClient = mockk<BraintreeClient>(relaxed = true)
            val service = PaymentActionsService(braintreeClient)

            service.signalCustomerActionComplete(
                CustomerCompletedAction.Challenge(cardinalAuthenticationJWT = "jwt-value"),
            )
            advanceUntilIdle()

            coVerify {
                braintreeClient.sendGraphQLPOST(withArg { actualRequestBody ->
                    val customerActionComplete = actualRequestBody
                        .getJSONObject("variables")
                        .getJSONObject("input")
                        .getJSONObject("customerActionComplete")
                    assertEquals("jwt-value", customerActionComplete.getJSONObject("challenge").getString("cardinalAuthenticationJWT"))
                    assertTrue(!customerActionComplete.has("deviceDataCollection"))
                })
            }
        }

    @Test
    fun `when signalCustomerActionComplete succeeds, Success is returned with the resulting PaymentAction`() =
        runTest(testDispatcher) {
            val responseBody = """
                {
                    "data": {
                        "signalCustomerActionComplete": {
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
            val result = service.signalCustomerActionComplete(
                CustomerCompletedAction.DeviceDataCollection(dfReferenceId = "df-reference-id"),
            )
            advanceUntilIdle()

            val success = result as PaymentActionServiceResult.Success
            assertEquals("pa123", success.paymentAction.id)
            assertEquals(PaymentActionStatus.READY_FOR_CONFIRMATION, success.paymentAction.status)
        }

    @Test
    fun `when signalCustomerActionComplete's BraintreeClient throws an IOException, Failure is returned`() =
        runTest(testDispatcher) {
            val error = IOException("Network error")

            val braintreeClient = MockkBraintreeClientBuilder()
                .sendGraphQLPostErrorResponse(error)
                .build()

            val service = PaymentActionsService(braintreeClient)
            val result = service.signalCustomerActionComplete(
                CustomerCompletedAction.Challenge(cardinalAuthenticationJWT = "jwt-value"),
            )
            advanceUntilIdle()

            assertTrue(result is PaymentActionServiceResult.Failure)
            assertEquals(error, (result as PaymentActionServiceResult.Failure).error)
        }

    private fun String.normalizeWhitespace(): String = trim().replace(Regex("\\s+"), " ")
}
