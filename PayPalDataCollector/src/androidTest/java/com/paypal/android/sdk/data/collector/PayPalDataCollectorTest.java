package com.paypal.android.sdk.data.collector;

import androidx.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class PayPalDataCollectorTest {

    @Test
    public void getClientMetadataId_returnsClientMetadataId() {
        String clientMetadataId = PayPalDataCollector.getClientMetadataId(getTargetContext());

        assertFalse(TextUtils.isEmpty(clientMetadataId));
    }

    @Test
    public void getClientMetadataId_returnsPairingId() {
        String clientMetadataId = PayPalDataCollector.getClientMetadataId(getTargetContext(), "pairing-id");

        assertEquals("pairing-id", clientMetadataId);
    }
}
