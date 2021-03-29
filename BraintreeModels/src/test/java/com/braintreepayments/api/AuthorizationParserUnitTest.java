package com.braintreepayments.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AuthorizationParserUnitTest {

    @Test
    public void parse_returnsValidClientTokenWhenBase64() throws InvalidArgumentException {
        AuthorizationParser sut = new AuthorizationParser();
        Authorization authorization = sut.parse(Fixtures.BASE64_CLIENT_TOKEN);

        assertTrue(authorization instanceof ClientToken);
    }

    @Test
    public void parse_returnsValidTokenizationKey() throws InvalidArgumentException {
        AuthorizationParser sut = new AuthorizationParser();
        Authorization authorization = sut.parse(Fixtures.TOKENIZATION_KEY);

        assertTrue(authorization instanceof TokenizationKey);
    }

    @Test
    public void parse_returnsValidPayPalUAT() throws InvalidArgumentException {
        AuthorizationParser sut = new AuthorizationParser();
        Authorization authorization = sut.parse(Fixtures.BASE64_PAYPAL_UAT);

        assertTrue(authorization instanceof PayPalUAT);
    }

    @Test(expected = InvalidArgumentException.class)
    public void parse_throwsWhenPassedNull() throws InvalidArgumentException {
        AuthorizationParser sut = new AuthorizationParser();
        sut.parse(null);
    }

    @Test(expected = InvalidArgumentException.class)
    public void parse_throwsWhenPassedAnEmptyString() throws InvalidArgumentException {
        AuthorizationParser sut = new AuthorizationParser();
        sut.parse("");
    }

    @Test(expected = InvalidArgumentException.class)
    public void parse_throwsWhenPassedJunk() throws InvalidArgumentException {
        AuthorizationParser sut = new AuthorizationParser();
        sut.parse("not authorization");
    }
}
