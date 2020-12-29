package com.braintreepayments.api;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class PayPalDataCollectorRequestUnitTest {

    @Test
    public void setClientMetadataId_trimsId_to_32characters() {
        PayPalDataCollectorRequest request = new PayPalDataCollectorRequest()
                .setClientMetadataId("pairing-id-pairing-id-pairing-idXXX");

        assertEquals("pairing-id-pairing-id-pairing-id", request.getClientMetadataId());
        assertEquals(32, request.getClientMetadataId().length());
    }
}
