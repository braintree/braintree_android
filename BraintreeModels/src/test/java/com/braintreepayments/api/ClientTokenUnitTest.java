package com.braintreepayments.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.FixturesHelper.base64Encode;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ClientTokenUnitTest {
    @Test
    public void fromString_deserializesClientToken() {
        ClientToken clientToken = (ClientToken) Authorization
                .fromString(base64Encode(Fixtures.CLIENT_TOKEN));

        assertEquals("client_api_configuration_url", clientToken.getConfigUrl());
        assertEquals("authorization_fingerprint", clientToken.getAuthorizationFingerprint());
    }

    @Test
    public void fromString_canDeserializeFromBase64String() {
        ClientToken clientToken = (ClientToken) Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN);

        assertEquals("encoded_capi_configuration_url", clientToken.getConfigUrl());
        assertEquals("encoded_auth_fingerprint", clientToken.getAuthorizationFingerprint());
    }

    @Test
    public void fromString_returnsInvalidTokenWhenGivenRandomJson() {
        Authorization result = ClientToken.fromString(Fixtures.RANDOM_JSON);

        assertTrue(result instanceof InvalidToken);
    }

    @Test
    public void getBearer_returnsAuthorizationFingerprint() {
        ClientToken clientToken = (ClientToken) Authorization
                .fromString(base64Encode(Fixtures.CLIENT_TOKEN));

        assertEquals(clientToken.getAuthorizationFingerprint(), clientToken.getBearer());
    }

    @Test
    public void getCustomerId_returnsNull_whenCustomerIdNotPresent() {
        ClientToken clientToken = (ClientToken) Authorization
                .fromString(base64Encode(Fixtures.CLIENT_TOKEN_WITH_AUTHORIZATION_FINGERPRINT_OPTIONS));

        assertNull(clientToken.getCustomerId());
    }

    @Test
    public void getCustomerId_returnsCustomerId() {
        ClientToken clientToken = (ClientToken) Authorization
                .fromString(base64Encode(Fixtures.CLIENT_TOKEN_WITH_CUSTOMER_ID_IN_AUTHORIZATION_FINGERPRINT));

        assertEquals("fake-customer-123", clientToken.getCustomerId());
    }
}
