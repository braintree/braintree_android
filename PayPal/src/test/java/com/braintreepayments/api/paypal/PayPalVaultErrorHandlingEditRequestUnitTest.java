package com.braintreepayments.api.paypal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import com.braintreepayments.api.paypal.vaultedit.PayPalVaultErrorHandlingEditRequest;

import org.junit.Test;

public class PayPalVaultErrorHandlingEditRequestUnitTest {
    @Test
    public void newPayPalVaultEditRequest_setsDefaultValues() {
        String editVaultId = "+fZXfUn6nzR+M9661WGnCBfyPlIExIMPY2rS9AC2vmA=";
        String correlationId = "test";
        PayPalVaultErrorHandlingEditRequest request = new PayPalVaultErrorHandlingEditRequest(editVaultId, correlationId);

        assertNotNull(request.getEditPayPalVaultId());
        assertNotNull(request.getRiskCorrelationId());

        assertEquals(request.getEditPayPalVaultId(), editVaultId);
        assertEquals(request.getRiskCorrelationId(), correlationId);
    }
}