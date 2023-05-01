package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.TestDriver;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(AndroidJUnit4ClassRunner.class)
public class AnalyticsClientTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();

        androidx.work.Configuration workManagerConfig = new androidx.work.Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, workManagerConfig);
    }

    @Test(timeout = 10000)
    public void sendsCorrectlyFormattedAnalyticsRequestToSandbox() throws Exception {
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SANDBOX_ANALYTICS);

        AnalyticsClient sut = new AnalyticsClient(context);
        UUID workSpecId = sut.sendEvent(configuration, "event.started", "sessionId", "custom", 123, authorization);

        WorkInfo workInfoBeforeDelay = WorkManager.getInstance(context).getWorkInfoById(workSpecId).get();
        assertEquals(workInfoBeforeDelay.getState(), WorkInfo.State.ENQUEUED);

        TestDriver testDriver = WorkManagerTestInitHelper.getTestDriver(context);
        testDriver.setInitialDelayMet(workSpecId);

        WorkInfo workInfoAfterDelay = WorkManager.getInstance(context).getWorkInfoById(workSpecId).get();
        assertEquals(workInfoAfterDelay.getState(), WorkInfo.State.SUCCEEDED);
    }

    @Test(timeout = 10000)
    public void sendsCorrectlyFormattedAnalyticsRequestToProd() throws Exception {
        Authorization authorization = Authorization.fromString(Fixtures.PROD_TOKENIZATION_KEY);
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PROD_ANALYTICS);

        AnalyticsClient sut = new AnalyticsClient(context);
        UUID workSpecId = sut.sendEvent(configuration, "event.started", "sessionId", "custom", 123, authorization);

        WorkInfo workInfoBeforeDelay = WorkManager.getInstance(context).getWorkInfoById(workSpecId).get();
        assertEquals(workInfoBeforeDelay.getState(), WorkInfo.State.ENQUEUED);

        TestDriver testDriver = WorkManagerTestInitHelper.getTestDriver(context);
        testDriver.setInitialDelayMet(workSpecId);

        WorkInfo workInfoAfterDelay = WorkManager.getInstance(context).getWorkInfoById(workSpecId).get();
        assertEquals(workInfoAfterDelay.getState(), WorkInfo.State.SUCCEEDED);
    }
}
