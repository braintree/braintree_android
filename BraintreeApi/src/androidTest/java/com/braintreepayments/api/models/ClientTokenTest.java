package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.testutils.FixturesHelper;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ClientTokenTest {

    @Test(timeout = 1000)
    @SmallTest
    public void fromString_deserializesClientToken() throws JSONException {
        ClientToken clientToken = ClientToken.fromString(
                FixturesHelper.stringFromFixture(getTargetContext(), "client_token.json"));

        assertEquals("client_api_configuration_url", clientToken.getConfigUrl());
        assertEquals("authorization_fingerprint", clientToken.getAuthorizationFingerprint());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromString_canDeserializeFromBase64String() throws JSONException {
        ClientToken clientToken = ClientToken.fromString(
                FixturesHelper.stringFromFixture(getTargetContext(), "base_64_client_token.txt"));

        assertEquals("encoded_capi_configuration_url", clientToken.getConfigUrl());
        assertEquals("encoded_auth_fingerprint", clientToken.getAuthorizationFingerprint());
    }

    @Test(timeout = 1000, expected = JSONException.class)
    @SmallTest
    public void fromString_throwsJSONExceptionWhenGivenRandomJson() throws JSONException {
        ClientToken.fromString(
                FixturesHelper.stringFromFixture(getTargetContext(), "random_json.json"));
    }
}
