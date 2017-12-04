package com.braintreepayments.api.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;

import static com.braintreepayments.testutils.FixturesHelper.streamFromString;
import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class StreamHelperUnitTest {

    @Test
    public void getString_readsAStringFromAStream() throws IOException {
        InputStream inputStream = streamFromString("Test string");

        assertEquals("Test string", StreamHelper.getString(inputStream));
    }
}
