package com.braintreepayments.api

import com.braintreepayments.api.Authorization.Companion.fromString
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class AuthorizationUnitTest {
    @Test
    fun fromString_returnsValidClientTokenWhenBase64() {
        val authorization = fromString(Fixtures.BASE64_CLIENT_TOKEN)
        assertTrue(authorization is ClientToken)
    }

    @Test
    fun fromString_returnsValidClientTokenWhenBase64IncludesSpaces() {
        val authorization = fromString(Fixtures.BASE64_CLIENT_TOKEN_WITH_SPACES)
        assertTrue(authorization is ClientToken)
    }

    @Test
    fun fromString_returnsValidTokenizationKey() {
        val authorization = fromString(Fixtures.TOKENIZATION_KEY)
        assertTrue(authorization is TokenizationKey)
    }

    @Test
    fun fromString_returnsValidTokenizationKeyIncludesSpaces() {
        val authorization = fromString(Fixtures.TOKENIZATION_KEY_WITH_SPACES)
        assertTrue(authorization is TokenizationKey)
    }

    @Test
    fun fromString_whenPassedNull_returnsInvalidToken() {
        val result = fromString(null)
        assertTrue(result is InvalidAuthorization)
    }

    @Test
    fun fromString_whenPassedAnEmptyString_returnsInvalidToken() {
        val result = fromString("")
        assertTrue(result is InvalidAuthorization)
    }

    @Test
    fun fromString_whenPassedJunk_returnsInvalidToken() {
        val result = fromString("not authorization")
        assertTrue(result is InvalidAuthorization)
    }
}
