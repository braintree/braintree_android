package com.braintreepayments.api.datacollector;

import static junit.framework.Assert.assertFalse;

import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.testutils.Fixtures;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import kotlin.Unit;

@RunWith(AndroidJUnit4ClassRunner.class)
public class DataCollectorTest {

    @Test(timeout = 10000)
    public void getClientMetadataId_returnsClientMetadataId() throws JSONException, InterruptedException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        DataCollector sut = new DataCollector(ApplicationProvider.getApplicationContext(), Fixtures.TOKENIZATION_KEY);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        sut.getClientMetadataId(ApplicationProvider.getApplicationContext(), configuration, true, (String clientMetadataId) -> {
            assertFalse(TextUtils.isEmpty(clientMetadataId));
            countDownLatch.countDown();
            return Unit.INSTANCE;
        });

        countDownLatch.await();
    }
}

