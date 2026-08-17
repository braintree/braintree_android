package com.braintreepayments.api.core

import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.FixturesHelper
import com.braintreepayments.api.core.Authorization.Companion.fromString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClientTokenUnitTest {
    @Test
    fun `when given a base64-encoded CLIENT_TOKEN fixture, fromString deserializes it into a ClientToken`() {
        val clientToken =
            fromString(FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)) as ClientToken
        assertEquals("client_api_configuration_url", clientToken.configUrl)
        assertEquals("authorization_fingerprint", clientToken.authorizationFingerprint)
    }

    @Test
    fun `when given the BASE64_CLIENT_TOKEN fixture, fromString deserializes the encoded fields`() {
        val clientToken = fromString(Fixtures.BASE64_CLIENT_TOKEN) as ClientToken
        assertEquals("encoded_capi_configuration_url", clientToken.configUrl)
        assertEquals("encoded_auth_fingerprint", clientToken.authorizationFingerprint)
    }

    @Test
    fun `when given random json, fromString returns InvalidAuthorization`() {
        val result = fromString(Fixtures.RANDOM_JSON)
        assertTrue(result is InvalidAuthorization)
    }

    @Test
    fun `when clientToken is decoded, bearer returns the authorization fingerprint`() {
        val clientToken =
            fromString(FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)) as ClientToken
        assertEquals(clientToken.authorizationFingerprint, clientToken.bearer)
    }

    @Test
    fun `when customer id is not present in authorization fingerprint options, customerId returns null`() {
        val clientToken =
            fromString(
                FixturesHelper.base64Encode(
                    Fixtures.CLIENT_TOKEN_WITH_AUTHORIZATION_FINGERPRINT_OPTIONS
                )
            ) as ClientToken
        assertNull(clientToken.customerId)
    }

    @Test
    fun `when customer id is present in authorization fingerprint, customerId returns it`() {
        val clientToken =
            fromString(
                FixturesHelper.base64Encode(
                    Fixtures.CLIENT_TOKEN_WITH_CUSTOMER_ID_IN_AUTHORIZATION_FINGERPRINT
                )
            ) as ClientToken
        assertEquals("fake-customer-123", clientToken.customerId)
    }
}
