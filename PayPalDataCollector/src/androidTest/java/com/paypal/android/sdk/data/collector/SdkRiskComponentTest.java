package com.paypal.android.sdk.data.collector;

import androidx.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

@SuppressWarnings("deprecation")
@RunWith(AndroidJUnit4.class)
public class SdkRiskComponentTest {

    private static final String GUID = UUID.randomUUID().toString();

    @Test
    public void getClientMetadataId_returnsClientMetadataId() {
        String clientMetadataId = SdkRiskComponent.getClientMetadataId(getTargetContext(), GUID, null);

        assertFalse(TextUtils.isEmpty(clientMetadataId));
    }

    @Test
    public void getClientMetadataId_returnsPairingId() {
        String clientMetadataId = SdkRiskComponent.getClientMetadataId(getTargetContext(), GUID, "pairing-id");

        assertEquals("pairing-id", clientMetadataId);
    }
}