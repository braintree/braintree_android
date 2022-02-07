package com.braintreepayments.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VenmoActivityResultContractUnitTest {
    @Test
    public void createIntent_returnsIntentWithExtras() {}

    @Test
    public void parseResult_whenResultIsOK_returnsVenmoResultWithNonce() {}

    @Test
    public void parseResult_whenResultIsOKAndIntentIsNull_returnsVenmoResultWithError() {}

    @Test
    public void parseResult_whenResultIsCANCELED_returnsVenomResultWithError() {}
}
