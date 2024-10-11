package com.braintreepayments.api.datacollector

import org.junit.Assert
import org.junit.Test

class DataCollectorInternalRequestUnitTest {
    @Test
    fun setClientMetadataId_trimsId_to_32characters() {
        val request = DataCollectorInternalRequest(true)
        request.clientMetadataId = "pairing-id-pairing-id-pairing-idXXX"

        Assert.assertEquals("pairing-id-pairing-id-pairing-id", request.clientMetadataId)
        Assert.assertEquals(32, request.clientMetadataId!!.length)
    }
}
