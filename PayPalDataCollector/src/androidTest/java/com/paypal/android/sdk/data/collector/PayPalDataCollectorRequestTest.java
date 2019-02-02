package com.paypal.android.sdk.data.collector;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.runner.AndroidJUnit4;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class PayPalDataCollectorRequestTest {

    @Test
    public void setClientMetadataId_trimsId_to_32characters() {
        PayPalDataCollectorRequest request = new PayPalDataCollectorRequest()
                .setClientMetadataId("pairing-id-pairing-id-pairing-idXXX");

        assertEquals("pairing-id-pairing-id-pairing-id", request.getClientMetadataId());
        assertEquals(32, request.getClientMetadataId().length());
    }
}
