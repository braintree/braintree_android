package com.paypal.android.sdk.onetouch.core;

import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(sdk = 16, constants = BuildConfig.class)
public class PerformRequestStatusTest {

    @Test
    public void constructsCorrectly() {
        PerformRequestStatus requestStatus = new PerformRequestStatus(true, RequestTarget.wallet,
                "client-metadata-id");

        assertTrue(requestStatus.isSuccess());
        assertEquals(RequestTarget.wallet, requestStatus.getRequestTarget());
        assertEquals("client-metadata-id", requestStatus.getClientMetadataId());
        assertEquals("PerformRequestStatus[mSuccess=true, mRequestTarget=wallet, mClientMetadataId=client-metadata-id]",
                requestStatus.toString());
    }
}
