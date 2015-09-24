package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.InvalidArgumentException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ClientTokenTest {

    @Test(timeout = 1000)
    public void fromString_deserializesClientToken() throws JSONException,
            InvalidArgumentException {
        ClientToken clientToken =
                (ClientToken) Authorization.fromString(stringFromFixture("client_token.json"));

        assertEquals("client_api_configuration_url", clientToken.getConfigUrl());
        assertEquals("authorization_fingerprint", clientToken.getAuthorizationFingerprint());
    }

    @Test(timeout = 1000)
    public void fromString_canDeserializeFromBase64String()
            throws JSONException, InvalidArgumentException {
        ClientToken clientToken = (ClientToken) Authorization.fromString(
                stringFromFixture("base_64_client_token.txt"));

        assertEquals("encoded_capi_configuration_url", clientToken.getConfigUrl());
        assertEquals("encoded_auth_fingerprint", clientToken.getAuthorizationFingerprint());
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void fromString_throwsInvalidArgumentExceptionWhenGivenRandomJson() throws InvalidArgumentException {
        ClientToken.fromString(stringFromFixture("random_json.json"));
    }
}
