package com.braintreepayments.api.paypal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditRequest;

import org.junit.Test;

public class PayPalVaultEditRequestUnitTest {
    @Test
    public void newPayPalVaultEditRequest_setsDefaultValues() {
        String editVaultId = "+fZXfUn6nzR+M9661WGnCBfyPlIExIMPY2rS9AC2vmA=";
        PayPalVaultEditRequest request = new PayPalVaultEditRequest(editVaultId);

        assertNotNull(request.getEditPayPalVaultId());
        assertEquals(request.getEditPayPalVaultId(), editVaultId);
    }
}

