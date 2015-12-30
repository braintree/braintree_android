package com.paypal.android.sdk.onetouch.core.sdk;

import android.content.Intent;

import com.paypal.android.sdk.onetouch.core.BuildConfig;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(sdk = 16, constants = BuildConfig.class)
public class PendingRequestTest {

    @Test
    public void constructsCorrectly() {
        Intent intent = new Intent();
        PendingRequest requestStatus = new PendingRequest(true, RequestTarget.wallet,
                "client-metadata-id", intent);

        assertTrue(requestStatus.isSuccess());
        assertEquals(RequestTarget.wallet, requestStatus.getRequestTarget());
        assertEquals("client-metadata-id", requestStatus.getClientMetadataId());
        assertEquals(intent, requestStatus.getIntent());
    }
}
