package com.braintreepayments.api.core

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.braintreepayments.api.core.Authorization.Companion.fromString
import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.testutils.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class AnalyticsClientTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val workManagerConfig = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, workManagerConfig)
    }

    @Test(timeout = 10000)
    @Throws(Exception::class)
    fun sendsCorrectlyFormattedAnalyticsRequestToWorkManager() {
        val authorization = fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)

        val event = AnalyticsEvent("event.started", 1234)
        val sut = AnalyticsClient(context)
        val workSpecId =
            sut.sendEvent(configuration, event, IntegrationType.CUSTOM, authorization)

        val workInfoBeforeDelay =
            WorkManager.getInstance(context).getWorkInfoById(workSpecId).get()
        assertEquals(workInfoBeforeDelay.state, WorkInfo.State.ENQUEUED)

        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!
        testDriver.setInitialDelayMet(workSpecId)

        val workInfoAfterDelay =
            WorkManager.getInstance(context).getWorkInfoById(workSpecId).get()
        assertEquals(workInfoAfterDelay.state, WorkInfo.State.SUCCEEDED)
    }
}
