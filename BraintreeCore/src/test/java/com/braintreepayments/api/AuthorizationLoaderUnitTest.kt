package com.braintreepayments.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthorizationLoaderUnitTest {

    private lateinit var sut: AuthorizationLoader

    @Test
    fun loadAuthorization_whenInitialAuthExists_callsBackAuth() {
        val initialAuthString = Fixtures.TOKENIZATION_KEY
        val callback = mockk<AuthorizationCallback>(relaxed = true)
        val authSlot = slot<Authorization>()
        every { callback.onAuthorizationResult(capture(authSlot), null) } returns Unit

        sut = AuthorizationLoader(initialAuthString, null)
        sut.loadAuthorization(callback)

        val authorization = authSlot.captured
        assertEquals(initialAuthString, authorization.toString())
    }

    @Test
    fun loadAuthorization_whenInitialAuthExistsAndInvalidateClientTokenCalled_returnsInitialValue() {
        val callback1 = mockk<AuthorizationCallback>(relaxed = true)
        val authSlot1 = slot<Authorization>()
        every { callback1.onAuthorizationResult(capture(authSlot1), null) } returns Unit

        val initialAuthString = Fixtures.TOKENIZATION_KEY
        sut = AuthorizationLoader(initialAuthString, null)
        sut.loadAuthorization(callback1)

        val authorization = authSlot1.captured
        assertEquals(initialAuthString, authorization.toString())

        sut.invalidateClientToken()

        val callback2 = mockk<AuthorizationCallback>()
        val authSlot2 = slot<Authorization>()
        every { callback2.onAuthorizationResult(capture(authSlot2), null) } returns Unit

        sut.loadAuthorization(callback2)
        val authorization2 = authSlot2.captured
        assertEquals(initialAuthString, authorization2.toString())
    }

    @Test
    fun loadAuthorization_whenInitialAuthDoesNotExist_callsBackSuccessfulClientTokenFetch() {
        val callback = mockk<AuthorizationCallback>()
        val authSlot = slot<Authorization>()
        every { callback.onAuthorizationResult(capture(authSlot), null) } returns Unit

        val clientToken = Fixtures.BASE64_CLIENT_TOKEN
        val clientTokenProvider = MockkClientTokenProviderBuilder()
            .clientToken(clientToken)
            .build()
        sut = AuthorizationLoader(null, clientTokenProvider)

        sut.loadAuthorization(callback)
        val authorization = authSlot.captured
        assertEquals(clientToken, authorization.toString())
    }

    @Test
    fun loadAuthorization_whenInitialAuthDoesNotExist_cachesClientTokenInMemory() {
        val clientToken = Fixtures.BASE64_CLIENT_TOKEN
        val clientTokenProvider = MockkClientTokenProviderBuilder()
            .clientToken(clientToken)
            .build()
        sut = AuthorizationLoader(null, clientTokenProvider)

        val callback = mockk<AuthorizationCallback>(relaxed = true)
        sut.loadAuthorization(callback)
        sut.loadAuthorization(callback)

        verify(exactly = 1) { clientTokenProvider.getClientToken(any()) }
    }

    @Test
    fun loadAuthorization_whenInitialAuthDoesNotExistAndInvalidateClientTokenCalled_returnsNewClientToken() {
        val clientToken1 = Fixtures.BASE64_CLIENT_TOKEN
        val clientToken2 = Fixtures.BASE64_CLIENT_TOKEN2
        val clientTokenProvider = MockkClientTokenProviderBuilder()
            .clientToken(clientToken1, clientToken2)
            .build()

        val callback1 = mockk<AuthorizationCallback>()
        val authSlot1 = slot<Authorization>()
        every { callback1.onAuthorizationResult(capture(authSlot1), null) } returns Unit

        sut = AuthorizationLoader(null, clientTokenProvider)
        sut.loadAuthorization(callback1)

        val auth1 = authSlot1.captured

        val callback2 = mockk<AuthorizationCallback>()
        val authSlot2 = slot<Authorization>()
        every { callback2.onAuthorizationResult(capture(authSlot2), null) } returns Unit

        sut.invalidateClientToken()
        sut.loadAuthorization(callback2)

        val auth2 = authSlot2.captured
        assertNotEquals(auth1.toString(), auth2.toString())
    }

    @Test
    fun loadAuthorization_whenInitialAuthDoesNotExistAndInvalidateClientTokenCalled_cachesNewClientTokenInMemory() {
        val clientToken = Fixtures.BASE64_CLIENT_TOKEN
        val clientTokenProvider = MockkClientTokenProviderBuilder()
            .clientToken(clientToken)
            .build()
        val callback = mockk<AuthorizationCallback>(relaxed = true)

        sut = AuthorizationLoader(null, clientTokenProvider)
        sut.loadAuthorization(callback)
        sut.invalidateClientToken()
        sut.loadAuthorization(callback)
        sut.loadAuthorization(callback)

        verify(exactly = 2) { clientTokenProvider.getClientToken(any()) }
    }

    @Test
    fun loadAuthorization_whenInitialAuthDoesNotExist_forwardsClientTokenFetchError() {
        val clientTokenFetchError = Exception("error")
        val clientTokenProvider = MockkClientTokenProviderBuilder()
            .error(clientTokenFetchError)
            .build()
        sut = AuthorizationLoader(null, clientTokenProvider)

        val callback = mockk<AuthorizationCallback>(relaxed = true)
        sut.loadAuthorization(callback)

        verify { callback.onAuthorizationResult(null, clientTokenFetchError) }
    }

    @Test
    fun loadAuthorization_whenInitialAuthDoesNotExistAndNoClientTokenProvider_callsBackException() {
        val callback = mockk<AuthorizationCallback>()
        val errorSlot = slot<BraintreeException>()
        every { callback.onAuthorizationResult(null, capture(errorSlot)) } returns Unit

        sut = AuthorizationLoader(null, null)
        sut.loadAuthorization(callback)

        val error = errorSlot.captured
        val expectedMessage =
            "Authorization required. See https://developer.paypal.com/braintree/docs/guides/" +
                    "client-sdk/setup/android/v4#initialization for more info."
        assertEquals(expectedMessage, error.message)
    }

    @Test
    fun authorizationFromCache_returnsInitialAuthorization() {
        val initialAuthString = Fixtures.TOKENIZATION_KEY
        sut = AuthorizationLoader(initialAuthString, null)
        assertEquals(initialAuthString, sut.authorizationFromCache?.toString())
    }

    @Test
    fun authorizationFromCache_returnsAuthorizationFromClientTokenProvider() {
        val clientToken = Fixtures.BASE64_CLIENT_TOKEN
        val clientTokenProvider = MockkClientTokenProviderBuilder()
            .clientToken(clientToken)
            .build()
        val callback = mockk<AuthorizationCallback>(relaxed = true)

        sut = AuthorizationLoader(null, clientTokenProvider)
        sut.loadAuthorization(callback)
        assertEquals(clientToken, sut.authorizationFromCache?.toString())
    }
}
