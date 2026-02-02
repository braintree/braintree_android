package com.braintreepayments.api.threedsecure

import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ThreeDSecureAPIUnitTest {
    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun performLookup_sendsPOSTRequest() = runTest(testDispatcher) {
        val urlSlot = slot<String>()
        val dataSlot = slot<String>()
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse("{}")
            .build()

        coEvery {
            braintreeClient.sendPOST(
                url = capture(urlSlot),
                data = capture(dataSlot)
            )
        } returns "{}"

        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )

        val threeDSecureRequest = mockk<ThreeDSecureRequest>(relaxed = true)
        val mockData = "{\"mock\":\"json\"}"
        every { threeDSecureRequest.nonce } returns "sample-nonce"
        every { threeDSecureRequest.build("cardinal-session-id") } returns mockData

        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)
        sut.performLookup(threeDSecureRequest, "cardinal-session-id", callback)
        advanceUntilIdle()
        assertEquals("/v1/payment_methods/sample-nonce/three_d_secure/lookup", urlSlot.captured)
        assertEquals(mockData, dataSlot.captured)
    }

    @Test
    fun performLookup_onSuccess_callbackThreeDSecureResult() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
            .build()
        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )

        val threeDSecureRequest = mockk<ThreeDSecureRequest>(relaxed = true)
        every { threeDSecureRequest.build(any()) } returns "{}"

        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)
        sut.performLookup(threeDSecureRequest, "another-session-id", callback)
        advanceUntilIdle()
        verify { callback.onThreeDSecureResult(any<ThreeDSecureParams>(), null) }
    }

    @Test
    fun performLookup_onInvalidJSONResponse_callbackJSONException() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse("invalid json")
            .build()
        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )

        val threeDSecureRequest = mockk<ThreeDSecureRequest>(relaxed = true)
        every { threeDSecureRequest.build(any()) } returns "{}"

        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)
        sut.performLookup(threeDSecureRequest, "cardinal-session-id", callback)
        advanceUntilIdle()
        verify { callback.onThreeDSecureResult(null, any<JSONException>()) }
    }

    @Test
    fun performLookup_onPOSTFailure_callbackHTTPError() = runTest(testDispatcher) {
        val httpError = IOException("http error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostErrorResponse(httpError)
            .build()
        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )

        val threeDSecureRequest = mockk<ThreeDSecureRequest>(relaxed = true)
        every { threeDSecureRequest.build(any()) } returns "{}"

        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)
        sut.performLookup(threeDSecureRequest, "cardinal-session-id", callback)
        advanceUntilIdle()
        verify { callback.onThreeDSecureResult(null, httpError) }
    }

    @Test
    fun authenticateCardinalJWT_sendsPOSTRequest() = runTest(testDispatcher) {
        val urlSlot = slot<String>()
        val dataSlot = slot<String>()
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse("{}")
            .build()

        coEvery {
            braintreeClient.sendPOST(
                url = capture(urlSlot),
                data = capture(dataSlot)
            )
        } returns "{}"

        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
        val cardinalJWT = "cardinal-jwt"

        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)
        sut.authenticateCardinalJWT(threeDSecureParams, cardinalJWT, callback)
        advanceUntilIdle()
        assertEquals(
            "/v1/payment_methods/123456-12345-12345-a-adfa/three_d_secure/authenticate_from_jwt",
            urlSlot.captured
        )

        val expectedJSON = JSONObject()
            .put("jwt", "cardinal-jwt")
            .put("paymentMethodNonce", "123456-12345-12345-a-adfa")
        JSONAssert.assertEquals(expectedJSON, JSONObject(dataSlot.captured), true)
    }

    @Test
    fun authenticateCardinalJWT_onSuccess_callbackThreeDSecureResult() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
            .build()
        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
        val cardinalJWT = "cardinal-jwt"

        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)
        sut.authenticateCardinalJWT(threeDSecureParams, cardinalJWT, callback)
        advanceUntilIdle()
        verify { callback.onThreeDSecureResult(any<ThreeDSecureParams>(), null) }
    }

    @Test
    fun authenticateCardinalJWT_onThreeDSecureError_callbackThreeDSecureResultWithOriginalLookupNonce() =
        runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE_WITH_ERROR)
            .build()
        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
        val cardinalJWT = "cardinal-jwt"

        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)
        sut.authenticateCardinalJWT(threeDSecureParams, cardinalJWT, callback)
        advanceUntilIdle()
        val captor = slot<ThreeDSecureParams>()
        verify { callback.onThreeDSecureResult(capture(captor), null) }

        val result = captor.captured
        assertNotNull(result.threeDSecureNonce)
    }

    @Test
    fun authenticateCardinalJWT_onInvalidJSONResponse_callbackJSONException() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse("not-json")
            .build()
        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
        val cardinalJWT = "cardinal-jwt"

        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)
        sut.authenticateCardinalJWT(threeDSecureParams, cardinalJWT, callback)
        advanceUntilIdle()
        val captor = slot<Exception>()
        verify { callback.onThreeDSecureResult(null, capture(captor)) }

        val error = captor.captured
        assertTrue(error is JSONException)
    }

    @Test
    fun authenticateCardinalJWT_onPOSTFailure_callbackHTTPError() = runTest(testDispatcher) {
        val postError = IOException("post-error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostErrorResponse(postError)
            .build()
        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
        val cardinalJWT = "cardinal-jwt"

        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)
        sut.authenticateCardinalJWT(threeDSecureParams, cardinalJWT, callback)
        advanceUntilIdle()
        val captor = slot<Exception>()
        verify { callback.onThreeDSecureResult(null, capture(captor)) }

        val error = captor.captured
        assertSame(postError, error)
    }

    @Test
    fun authenticateCardinalJWT_whenCustomerFailsAuthentication_returnsLookupCardNonce() = runTest(testDispatcher) {
        val authResponseJson = Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE_WITH_ERROR
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse(authResponseJson)
            .build()

        val threeDSecureParams = ThreeDSecureParams.fromJson(
            Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE_WITHOUT_LIABILITY_WITH_LIABILITY_SHIFT_POSSIBLE
        )
        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)

        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )
        sut.authenticateCardinalJWT(threeDSecureParams, "jwt", callback)
        advanceUntilIdle()
        val captor = slot<ThreeDSecureParams>()
        verify { callback.onThreeDSecureResult(capture(captor), null) }

        val actualResult = captor.captured
        val cardNonce = actualResult.threeDSecureNonce
        assertNotNull(cardNonce)

        val threeDSecureInfo = cardNonce.threeDSecureInfo
        assertFalse(threeDSecureInfo.liabilityShifted)
        assertTrue(threeDSecureInfo.liabilityShiftPossible)
        assertEquals("123456-12345-12345-a-adfa", cardNonce.string)
        assertEquals(
            "Failed to authenticate, please try a different form of payment.",
            actualResult.errorMessage
        )
    }

    @Test
    fun authenticateCardinalJWT_whenPostError_returnsException() = runTest(testDispatcher) {
        val exception = IOException("Error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostErrorResponse(exception)
            .build()

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)

        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )
        sut.authenticateCardinalJWT(threeDSecureParams, "jwt", callback)
        advanceUntilIdle()
        verify { callback.onThreeDSecureResult(null, exception) }
    }

    @Test
    fun authenticateCardinalJWT_whenJWTNull_returnsException() {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)

        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )
        sut.authenticateCardinalJWT(threeDSecureParams, null, callback)

        val captor = slot<BraintreeException>()
        verify { callback.onThreeDSecureResult(null, capture(captor)) }
        assertEquals("threeDSecureParams or jwt is null", captor.captured.message)
    }

    @Test
    fun authenticateCardinalJWT_whenThreeDSecureParamsNull_returnsException() {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val callback = mockk<ThreeDSecureResultCallback>(relaxed = true)

        val sut = ThreeDSecureAPI(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )
        sut.authenticateCardinalJWT(null, "jwt", callback)

        val captor = slot<BraintreeException>()
        verify { callback.onThreeDSecureResult(null, capture(captor)) }
        assertEquals("threeDSecureParams or jwt is null", captor.captured.message)
    }
}
