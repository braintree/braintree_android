package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class PayPalDataCollectorInternalRequestUnitTest {

    @Test
    public void setClientMetadataId_trimsId_to_32characters() {
        PayPalDataCollectorInternalRequest request = new PayPalDataCollectorInternalRequest(true)
                .setRiskCorrelationId("pairing-id-pairing-id-pairing-idXXX");

        assertEquals("pairing-id-pairing-id-pairing-id", request.getClientMetadataId());
        assertEquals(32, request.getClientMetadataId().length());
    }
}
