package com.braintreepayments.api;

import android.content.Context;
import android.util.Log;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.TestDriver;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4ClassRunner.class)
public class AnalyticsClientTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> activityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private TestActivity activity;

    @Before
    public void setUp() {
        activity = activityTestRule.getActivity();

        androidx.work.Configuration workManagerConfig = new androidx.work.Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(activity, workManagerConfig);
    }

    @Test(timeout = 10000)
    public void sendsCorrectlyFormattedAnalyticsRequestToSandbox() throws Exception {
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SANDBOX_ANALYTICS);

        Context context = activity.getApplicationContext();
        AnalyticsEvent event = new AnalyticsEvent(
                context, "sessionId", "custom", "event.started");

        AnalyticsClient sut = new AnalyticsClient(authorization);
        UUID workSpecId = sut.sendEventAndReturnId(context, configuration, event);

        WorkInfo workInfoBeforeDelay = WorkManager.getInstance(context).getWorkInfoById(workSpecId).get();
        assertEquals(workInfoBeforeDelay.getState(), WorkInfo.State.ENQUEUED);

        TestDriver testDriver = WorkManagerTestInitHelper.getTestDriver(activity);
        testDriver.setInitialDelayMet(workSpecId);

        WorkInfo workInfoAfterDelay = WorkManager.getInstance(context).getWorkInfoById(workSpecId).get();
        assertEquals(workInfoAfterDelay.getState(), WorkInfo.State.SUCCEEDED);
    }

    @Test(timeout = 10000)
    public void sendsCorrectlyFormattedAnalyticsRequestToProd() throws Exception {
        Authorization authorization = Authorization.fromString(Fixtures.PROD_TOKENIZATION_KEY);
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PROD_ANALYTICS);

        Context context = activity.getApplicationContext();
        AnalyticsEvent event = new AnalyticsEvent(
                context, "sessionId", "custom", "event.started");

        AnalyticsClient sut = new AnalyticsClient(authorization);
        UUID workSpecId = sut.sendEventAndReturnId(context, configuration, event);

        WorkInfo workInfoBeforeDelay = WorkManager.getInstance(context).getWorkInfoById(workSpecId).get();
        assertEquals(workInfoBeforeDelay.getState(), WorkInfo.State.ENQUEUED);

        TestDriver testDriver = WorkManagerTestInitHelper.getTestDriver(activity);
        testDriver.setInitialDelayMet(workSpecId);

        WorkInfo workInfoAfterDelay = WorkManager.getInstance(context).getWorkInfoById(workSpecId).get();
        assertEquals(workInfoAfterDelay.getState(), WorkInfo.State.SUCCEEDED);
    }
}