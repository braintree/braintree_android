package com.braintreepayments.api.datacollector;

import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.testutils.Fixtures;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertFalse;

@RunWith(AndroidJUnit4ClassRunner.class)
public class DataCollectorTest {

    @Test
    public void getClientMetadataId_returnsClientMetadataId() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        DataCollector sut = new DataCollector(ApplicationProvider.getApplicationContext(), Fixtures.TOKENIZATION_KEY);
        String clientMetadataId = sut.getClientMetadataId(ApplicationProvider.getApplicationContext(), configuration, true);

        assertFalse(TextUtils.isEmpty(clientMetadataId));
    }
}

