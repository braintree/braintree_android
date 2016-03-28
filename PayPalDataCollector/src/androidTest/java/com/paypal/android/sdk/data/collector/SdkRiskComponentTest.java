package com.paypal.android.sdk.data.collector;

import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class SdkRiskComponentTest {

    private static final String GUID = UUID.randomUUID().toString();

    @Test(timeout = 1000)
    public void getClientMetadataId_returnsClientMetadataId() {
        String clientMetadataId = SdkRiskComponent.getClientMetadataId(getTargetContext(), GUID, null);

        assertFalse(TextUtils.isEmpty(clientMetadataId));
    }

    @Test(timeout = 1000)
    public void getClientMetadataId_returnsPairingId() {
        String clientMetadataId = SdkRiskComponent.getClientMetadataId(getTargetContext(), GUID, "pairing-id");

        assertEquals("pairing-id", clientMetadataId);
    }
}