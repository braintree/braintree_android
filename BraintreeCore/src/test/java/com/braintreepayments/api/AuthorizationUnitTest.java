package com.braintreepayments.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AuthorizationUnitTest {

    @Test
    public void fromString_returnsValidClientTokenWhenBase64() {
        Authorization authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN);

        assertTrue(authorization instanceof ClientToken);
    }

    @Test
    public void fromString_returnsValidClientTokenWhenBase64IncludesSpaces() {
        Authorization authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN_WITH_SPACES);

        assertTrue(authorization instanceof ClientToken);
    }

    @Test
    public void fromString_returnsValidTokenizationKey() {
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);

        assertTrue(authorization instanceof TokenizationKey);
    }

    @Test
    public void fromString_returnsValidTokenizationKeyIncludesSpaces() {
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY_WITH_SPACES);

        assertTrue(authorization instanceof TokenizationKey);
    }

    @Test
    public void fromString_whenPassedNull_returnsInvalidToken() {
        Authorization result = Authorization.fromString(null);

        assertTrue(result instanceof InvalidAuthorization);
    }

    @Test
    public void fromString_whenPassedAnEmptyString_returnsInvalidToken() {
        Authorization result = Authorization.fromString("");

        assertTrue(result instanceof InvalidAuthorization);
    }

    @Test
    public void fromString_whenPassedJunk_returnsInvalidToken() {
        Authorization result = Authorization.fromString("not authorization");

        assertTrue(result instanceof InvalidAuthorization);
    }
}
