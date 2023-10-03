package com.braintreepayments.api

import com.braintreepayments.api.Authorization.Companion.fromString
import org.junit.Assert.*
import org.robolectric.RobolectricTestRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class ClientTokenUnitTest {
    @Test
    fun fromString_deserializesClientToken() {
        val clientToken =
            fromString(FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)) as ClientToken
        assertEquals("client_api_configuration_url", clientToken.configUrl)
        assertEquals("authorization_fingerprint", clientToken.authorizationFingerprint)
    }

    @Test
    fun fromString_canDeserializeFromBase64String() {
        val clientToken = fromString(Fixtures.BASE64_CLIENT_TOKEN) as ClientToken
        assertEquals("encoded_capi_configuration_url", clientToken.configUrl)
        assertEquals("encoded_auth_fingerprint", clientToken.authorizationFingerprint)
    }

    @Test
    fun fromString_returnsInvalidTokenWhenGivenRandomJson() {
        val result = fromString(Fixtures.RANDOM_JSON)
        assertTrue(result is InvalidAuthorization)
    }

    @Test
    fun getBearer_returnsAuthorizationFingerprint() {
        val clientToken =
            fromString(FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)) as ClientToken
        assertEquals(clientToken.authorizationFingerprint, clientToken.bearer)
    }

    @Test
    fun getCustomerId_returnsNull_whenCustomerIdNotPresent() {
        val clientToken =
            fromString(FixturesHelper.base64Encode(
                Fixtures.CLIENT_TOKEN_WITH_AUTHORIZATION_FINGERPRINT_OPTIONS)) as ClientToken
        assertNull(clientToken.customerId)
    }

    @Test
    fun getCustomerId_returnsCustomerId() {
        val clientToken =
            fromString(FixturesHelper.base64Encode(
                Fixtures.CLIENT_TOKEN_WITH_CUSTOMER_ID_IN_AUTHORIZATION_FINGERPRINT)) as ClientToken
        assertEquals("fake-customer-123", clientToken.customerId)
    }
}
