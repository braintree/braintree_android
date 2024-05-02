package com.braintreepayments.api.datacollector;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class DataCollectorInternalRequestUnitTest {

    @Test
    public void setClientMetadataId_trimsId_to_32characters() {
        DataCollectorInternalRequest request = new DataCollectorInternalRequest(true)
                .setRiskCorrelationId("pairing-id-pairing-id-pairing-idXXX");

        assertEquals("pairing-id-pairing-id-pairing-id", request.getClientMetadataId());
        assertEquals(32, request.getClientMetadataId().length());
    }
}
