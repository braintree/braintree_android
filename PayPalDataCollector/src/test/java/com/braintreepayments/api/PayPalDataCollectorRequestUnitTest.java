package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class PayPalDataCollectorRequestUnitTest {

    @Test
    public void setClientMetadataId_trimsId_to_32characters() {
        PayPalDataCollectorRequest request = new PayPalDataCollectorRequest()
                .setRiskCorrelationId("pairing-id-pairing-id-pairing-idXXX");

        assertEquals("pairing-id-pairing-id-pairing-id", request.getClientMetadataId());
        assertEquals(32, request.getClientMetadataId().length());
    }
}
