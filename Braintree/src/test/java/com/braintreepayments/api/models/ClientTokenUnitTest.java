package com.braintreepayments.api.models;

import com.braintreepayments.api.exceptions.InvalidArgumentException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.base64EncodedClientTokenFromFixture;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class ClientTokenUnitTest {
    @Test
    public void fromString_deserializesClientToken() throws InvalidArgumentException {
        ClientToken clientToken = (ClientToken) Authorization
                .fromString(base64EncodedClientTokenFromFixture("client_token.json"));

        assertEquals("client_api_configuration_url", clientToken.getConfigUrl());
        assertEquals("authorization_fingerprint", clientToken.getAuthorizationFingerprint());
    }

    @Test
    public void fromString_canDeserializeFromBase64String() throws InvalidArgumentException {
        ClientToken clientToken = (ClientToken) Authorization.fromString(stringFromFixture("base_64_client_token.txt"));

        assertEquals("encoded_capi_configuration_url", clientToken.getConfigUrl());
        assertEquals("encoded_auth_fingerprint", clientToken.getAuthorizationFingerprint());
    }

    @Test(expected = InvalidArgumentException.class)
    public void fromString_throwsInvalidArgumentExceptionWhenGivenRandomJson() throws InvalidArgumentException {
        ClientToken.fromString(stringFromFixture("random_json.json"));
    }

    @Test
    public void getBearer_returnsAuthorizationFingerprint() throws InvalidArgumentException {
        ClientToken clientToken = (ClientToken) Authorization
                .fromString(base64EncodedClientTokenFromFixture("client_token.json"));

        assertEquals(clientToken.getAuthorizationFingerprint(), clientToken.getBearer());
    }

    @Test
    public void getCustomerId_returnsNull_whenCustomerIdNotPresent() throws InvalidArgumentException {
        ClientToken clientToken = (ClientToken) Authorization
                .fromString(base64EncodedClientTokenFromFixture("client_token_with_authorization_fingerprint_options.json"));

        assertNull(clientToken.getCustomerId());
    }

    @Test
    public void getCustomerId_returnsCustomerId() throws InvalidArgumentException {
        ClientToken clientToken = (ClientToken) Authorization
                .fromString(base64EncodedClientTokenFromFixture("client_token_with_customer_id_in_authorization_fingerprint.json"));

        assertEquals("fake-customer-123", clientToken.getCustomerId());
    }
}
