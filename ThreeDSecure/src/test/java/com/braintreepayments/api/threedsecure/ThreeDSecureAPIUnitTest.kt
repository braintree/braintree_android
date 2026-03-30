package com.braintreepayments.api.threedsecure

import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

        val sut = ThreeDSecureAPI(braintreeClient)

        val threeDSecureRequest = mockk<ThreeDSecureRequest>(relaxed = true)
        val mockData = "{\"mock\":\"json\"}"
        every { threeDSecureRequest.nonce } returns "sample-nonce"
        every { threeDSecureRequest.build("cardinal-session-id") } returns mockData

        sut.performLookup(threeDSecureRequest, "cardinal-session-id")
        assertEquals("/v1/payment_methods/sample-nonce/three_d_secure/lookup", urlSlot.captured)
        assertEquals(mockData, dataSlot.captured)
    }

    @Test
    fun performLookup_onSuccess_returnsThreeDSecureResult() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
            .build()
        val sut = ThreeDSecureAPI(braintreeClient)

        val threeDSecureRequest = mockk<ThreeDSecureRequest>(relaxed = true)
        every { threeDSecureRequest.build(any()) } returns "{}"

        val result = sut.performLookup(threeDSecureRequest, "another-session-id")
        assertNotNull(result)
    }

    @Test
    fun performLookup_onInvalidJSONResponse_throwsJSONException() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse("invalid json")
            .build()
        val sut = ThreeDSecureAPI(braintreeClient)

        val threeDSecureRequest = mockk<ThreeDSecureRequest>(relaxed = true)
        every { threeDSecureRequest.build(any()) } returns "{}"

        assertFailsWith<JSONException> {
            sut.performLookup(threeDSecureRequest, "cardinal-session-id")
        }
    }

    @Test
    fun performLookup_onPOSTFailure_throwsHTTPError() = runTest(testDispatcher) {
        val httpError = IOException("http error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostErrorResponse(httpError)
            .build()
        val sut = ThreeDSecureAPI(braintreeClient)

        val threeDSecureRequest = mockk<ThreeDSecureRequest>(relaxed = true)
        every { threeDSecureRequest.build(any()) } returns "{}"

        try {
            sut.performLookup(threeDSecureRequest, "cardinal-session-id")
            throw AssertionError("Expected IOException")
        } catch (e: IOException) {
            assertEquals(httpError, e)
        }
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

        val sut = ThreeDSecureAPI(braintreeClient)

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
        val cardinalJWT = "cardinal-jwt"

        sut.authenticateCardinalJWT(threeDSecureParams, cardinalJWT)
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
    fun authenticateCardinalJWT_onSuccess_returnsThreeDSecureResult() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
            .build()
        val sut = ThreeDSecureAPI(braintreeClient)

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
        val cardinalJWT = "cardinal-jwt"

        val result = sut.authenticateCardinalJWT(threeDSecureParams, cardinalJWT)
        assertNotNull(result)
    }

    @Test
    fun authenticateCardinalJWT_onThreeDSecureError_returnsResultWithOriginalLookupNonce() =
        runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE_WITH_ERROR)
            .build()
        val sut = ThreeDSecureAPI(braintreeClient)

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
        val cardinalJWT = "cardinal-jwt"

        val result = sut.authenticateCardinalJWT(threeDSecureParams, cardinalJWT)
        assertNotNull(result.threeDSecureNonce)
    }

    @Test
    fun authenticateCardinalJWT_onInvalidJSONResponse_throwsJSONException() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse("not-json")
            .build()
        val sut = ThreeDSecureAPI(braintreeClient)

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
        val cardinalJWT = "cardinal-jwt"

        assertFailsWith<JSONException> {
            sut.authenticateCardinalJWT(threeDSecureParams, cardinalJWT)
        }
    }

    @Test
    fun authenticateCardinalJWT_onPOSTFailure_throwsHTTPError() = runTest(testDispatcher) {
        val postError = IOException("post-error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostErrorResponse(postError)
            .build()
        val sut = ThreeDSecureAPI(braintreeClient)

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
        val cardinalJWT = "cardinal-jwt"

        try {
            sut.authenticateCardinalJWT(threeDSecureParams, cardinalJWT)
            throw AssertionError("Expected IOException")
        } catch (e: IOException) {
            assertEquals(postError, e)
        }
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

        val sut = ThreeDSecureAPI(braintreeClient)
        val result = sut.authenticateCardinalJWT(threeDSecureParams, "jwt")

        val cardNonce = result.threeDSecureNonce
        assertNotNull(cardNonce)

        val threeDSecureInfo = cardNonce.threeDSecureInfo
        assertFalse(threeDSecureInfo.liabilityShifted)
        assertTrue(threeDSecureInfo.liabilityShiftPossible)
        assertEquals("123456-12345-12345-a-adfa", cardNonce.string)
        assertEquals(
            "Failed to authenticate, please try a different form of payment.",
            result.errorMessage
        )
    }

    @Test
    fun authenticateCardinalJWT_whenPostError_throwsException() = runTest(testDispatcher) {
        val exception = IOException("Error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostErrorResponse(exception)
            .build()

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)

        val sut = ThreeDSecureAPI(braintreeClient)
        try {
            sut.authenticateCardinalJWT(threeDSecureParams, "jwt")
            throw AssertionError("Expected IOException")
        } catch (e: IOException) {
            assertEquals(exception, e)
        }
    }

    @Test
    fun authenticateCardinalJWT_whenJWTNull_throwsException() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)

        val sut = ThreeDSecureAPI(braintreeClient)
        val error = assertFailsWith<BraintreeException> {
            sut.authenticateCardinalJWT(threeDSecureParams, null)
        }
        assertEquals("threeDSecureParams or jwt is null", error.message)
    }

    @Test
    fun authenticateCardinalJWT_whenThreeDSecureParamsNull_throwsException() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = ThreeDSecureAPI(braintreeClient)
        val error = assertFailsWith<BraintreeException> {
            sut.authenticateCardinalJWT(null, "jwt")
        }
        assertEquals("threeDSecureParams or jwt is null", error.message)
    }
}
