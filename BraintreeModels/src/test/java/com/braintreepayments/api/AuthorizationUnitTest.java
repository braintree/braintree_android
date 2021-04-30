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
    public void fromString_returnsValidTokenizationKey() {
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);

        assertTrue(authorization instanceof TokenizationKey);
    }

    @Test
    public void fromString_returnsValidPayPalUAT() {
        Authorization authorization = Authorization.fromString(Fixtures.BASE64_PAYPAL_UAT);

        assertTrue(authorization instanceof PayPalUAT);
    }

    @Test(expected = InvalidArgumentException.class)
    public void fromString_throwsWhenPassedNull() {
        Authorization.fromString(null);
    }

    @Test(expected = InvalidArgumentException.class)
    public void fromString_throwsWhenPassedAnEmptyString() {
        Authorization.fromString("");
    }

    @Test(expected = InvalidArgumentException.class)
    public void fromString_throwsWhenPassedJunk() {
        Authorization.fromString("not authorization");
    }
}
