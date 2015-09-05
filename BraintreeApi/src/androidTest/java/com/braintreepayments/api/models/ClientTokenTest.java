package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import com.braintreepayments.testutils.FixturesHelper;

public class ClientTokenTest extends AndroidTestCase {

    public void testDeserializesFromClientToken() {
        ClientToken clientToken = ClientToken.fromString(
                FixturesHelper.stringFromFixture(getContext(), "client_token.json"));

        assertEquals("client_api_configuration_url", clientToken.getConfigUrl());
        assertEquals("authorization_fingerprint", clientToken.getAuthorizationFingerprint());
    }

    public void testCanDeserializeFromBase64String() {
        ClientToken clientToken = ClientToken.fromString(
                FixturesHelper.stringFromFixture(getContext(), "base_64_client_token.txt"));

        assertEquals("encoded_capi_configuration_url", clientToken.getConfigUrl());
        assertEquals("encoded_auth_fingerprint", clientToken.getAuthorizationFingerprint());
    }

    public void testReturnsClientTokenWithNullFieldsFromRandomJson() {
        ClientToken clientToken = ClientToken.fromString(
                FixturesHelper.stringFromFixture(getContext(), "random_json.json"));

        assertNull(clientToken.getConfigUrl());
        assertNull(clientToken.getAuthorizationFingerprint());
    }
}
