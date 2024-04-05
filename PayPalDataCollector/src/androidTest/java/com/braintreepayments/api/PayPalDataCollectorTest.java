package com.braintreepayments.api;

import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertFalse;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PayPalDataCollectorTest {

    @Test
    public void getClientMetadataId_returnsClientMetadataId() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        BraintreeClient braintreeClient = new BraintreeClient(ApplicationProvider.getApplicationContext(), Fixtures.TOKENIZATION_KEY);
        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient);
        String clientMetadataId = sut.getClientMetadataId(ApplicationProvider.getApplicationContext(), configuration, true);

        assertFalse(TextUtils.isEmpty(clientMetadataId));
    }
}

