package com.braintreepayments.api.datacollector;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class DataCollectorRequestUnitTest {

    @Test
    public void setClientMetadataId_trimsId_to_32characters() {
        DataCollectorRequest request = new DataCollectorRequest();

        assertEquals("pairing-id-pairing-id-pairing-id", request.getClientMetadataId());
        assertEquals(32, request.getClientMetadataId().length());
    }
}
