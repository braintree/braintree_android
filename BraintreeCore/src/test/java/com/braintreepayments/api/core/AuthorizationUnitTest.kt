package com.braintreepayments.api.core

import com.braintreepayments.api.core.Authorization.Companion.fromString
import com.braintreepayments.api.testutils.Fixtures
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class AuthorizationUnitTest {
    @Test
    fun `when given a base64 client token, fromString returns a ClientToken`() {
        val authorization = fromString(Fixtures.BASE64_CLIENT_TOKEN)
        assertTrue(authorization is ClientToken)
    }

    @Test
    fun `when given a base64 client token with spaces, fromString returns a ClientToken`() {
        val authorization = fromString(Fixtures.BASE64_CLIENT_TOKEN_WITH_SPACES)
        assertTrue(authorization is ClientToken)
    }

    @Test
    fun `when given a tokenization key, fromString returns a TokenizationKey`() {
        val authorization = fromString(Fixtures.TOKENIZATION_KEY)
        assertTrue(authorization is TokenizationKey)
    }

    @Test
    fun `when given a tokenization key with spaces, fromString returns a TokenizationKey`() {
        val authorization = fromString(Fixtures.TOKENIZATION_KEY_WITH_SPACES)
        assertTrue(authorization is TokenizationKey)
    }

    @Test
    fun `when passed null, fromString returns InvalidAuthorization`() {
        val result = fromString(null)
        assertTrue(result is InvalidAuthorization)
    }

    @Test
    fun `when passed an empty string, fromString returns InvalidAuthorization`() {
        val result = fromString("")
        assertTrue(result is InvalidAuthorization)
    }

    @Test
    fun `when passed a junk string, fromString returns InvalidAuthorization`() {
        val result = fromString("not authorization")
        assertTrue(result is InvalidAuthorization)
    }
}
