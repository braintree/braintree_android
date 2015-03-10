package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import com.braintreepayments.testutils.FixturesHelper;

public class ClientTokenTest extends AndroidTestCase {

    public void testDeserializesFromClientToken() {
        ClientToken clientToken = ClientToken.fromString(
                FixturesHelper.stringFromFixture(getContext(),
                        "client_token.json"));

        assertEquals("client_api_url", clientToken.getClientApiUrl());
        assertEquals("authorization_fingerprint", clientToken.getAuthorizationFingerprint());
    }

    public void testCanDeserializeFromBase64String() {
        ClientToken clientToken = ClientToken.fromString(
                FixturesHelper.stringFromFixture(getContext(),
                        "base_64_client_token.txt"));

        assertEquals("encoded_capi_url", clientToken.getClientApiUrl());
        assertEquals("encoded_auth_fingerprint", clientToken.getAuthorizationFingerprint());
    }

    public void testReturnsClientTokenWithNullFieldsFromRandomJson() {
        ClientToken clientToken = ClientToken.fromString(
                FixturesHelper.stringFromFixture(getContext(), "random_json.json"));

        assertNull(clientToken.getClientApiUrl());
        assertNull(clientToken.getAuthorizationFingerprint());
    }
}
