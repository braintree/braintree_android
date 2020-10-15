package com.braintreepayments.api;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PayPalDataCollectorRequestTest {

    @Test
    public void setClientMetadataId_trimsId_to_32characters() {
        PayPalDataCollectorRequest request = new PayPalDataCollectorRequest()
                .setClientMetadataId("pairing-id-pairing-id-pairing-idXXX");

        assertEquals("pairing-id-pairing-id-pairing-id", request.getClientMetadataId());
        assertEquals(32, request.getClientMetadataId().length());
    }
}
