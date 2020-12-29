package com.braintreepayments.api;

import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PayPalDataCollectorTest {

    @Test
    public void getClientMetadataId_returnsClientMetadataId() {
        PayPalDataCollector sut = new PayPalDataCollector();
        String clientMetadataId = sut.getClientMetadataId(ApplicationProvider.getApplicationContext());

        assertFalse(TextUtils.isEmpty(clientMetadataId));
    }

    @Test
    public void getClientMetadataId_returnsPairingId() {
        PayPalDataCollector sut = new PayPalDataCollector();
        String clientMetadataId = sut.getClientMetadataId(ApplicationProvider.getApplicationContext(), "pairing-id");

        assertEquals("pairing-id", clientMetadataId);
    }
}
