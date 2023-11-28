package com.braintreepayments.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.*
import com.braintreepayments.api.Authorization.Companion.fromString
import com.braintreepayments.api.Configuration.Companion.fromJson
import io.mockk.*
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert
import java.io.IOException
import java.security.GeneralSecurityException

@RunWith(RobolectricTestRunner::class)
class AnalyticsClientUnitTest {

    private lateinit var context: Context
    private lateinit var configuration: Configuration
    private lateinit var authorization: Authorization
    private lateinit var httpClient: BraintreeHttpClient
    private lateinit var deviceInspector: DeviceInspector
    private lateinit var eventName: String
    private lateinit var sessionId: String
    private lateinit var integration: String
    private lateinit var workManager: WorkManager
    private lateinit var analyticsDatabase: AnalyticsDatabase
    private lateinit var analyticsEventDao: AnalyticsEventDao

    private var timestamp: Long = 0

    @Before
    @Throws(InvalidArgumentException::class, GeneralSecurityException::class, IOException::class)
    fun beforeEach() {
        timestamp = 123
        eventName = "sample-event-name"
        sessionId = "sample-session-id"
        integration = "sample-integration"
        authorization = fromString(Fixtures.TOKENIZATION_KEY)
        context = ApplicationProvider.getApplicationContext()
        configuration = fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
        httpClient = mockk(relaxed = true)
        deviceInspector = mockk(relaxed = true)
        analyticsDatabase = mockk(relaxed = true)
        analyticsEventDao = mockk(relaxed = true)
        workManager = mockk(relaxed = true)

        every { analyticsDatabase.analyticsEventDao() } returns analyticsEventDao
    }

    @Test
    @Throws(JSONException::class)
    fun sendEvent_enqueuesAnalyticsWriteToDbWorker() {
        val workRequestSlot = slot<OneTimeWorkRequest>()
        every {
            workManager.enqueueUniqueWork(
                "writeAnalyticsToDb",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                capture(workRequestSlot)
            )
        } returns mockk()

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        sut.sendEvent(configuration, eventName, sessionId, integration, 123, authorization)

        val workSpec = workRequestSlot.captured.workSpec
        assertEquals(AnalyticsWriteToDbWorker::class.java.name, workSpec.workerClassName)
        assertEquals(authorization.toString(), workSpec.input.getString("authorization"))
        assertEquals("android.sample-event-name", workSpec.input.getString("eventName"))
        assertEquals(123, workSpec.input.getLong("timestamp", 0))
    }

    @Test
    @Throws(JSONException::class)
    fun sendEvent_enqueuesAnalyticsUploadWorker() {
        val workRequestSlot = slot<OneTimeWorkRequest>()
        every {
            workManager.enqueueUniqueWork(
                "uploadAnalytics",
                ExistingWorkPolicy.KEEP,
                capture(workRequestSlot)
            )
        } returns mockk()

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        sut.sendEvent(configuration, eventName, sessionId, integration, 123, authorization)

        val workSpec = workRequestSlot.captured.workSpec
        assertEquals(30000, workSpec.initialDelay)
        assertEquals(AnalyticsUploadWorker::class.java.name, workSpec.workerClassName)
        assertEquals(configuration.toJson(), workSpec.input.getString("configuration"))
        assertEquals(authorization.toString(), workSpec.input.getString("authorization"))
        assertEquals("sample-session-id", workSpec.input.getString("sessionId"))
        assertEquals("sample-integration", workSpec.input.getString("integration"))
    }

    @Test
    fun writeAnalytics_whenEventNameAndTimestampArePresent_returnsSuccess() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_EVENT_NAME, eventName)
            .putLong(AnalyticsClient.WORK_INPUT_KEY_TIMESTAMP, timestamp)
            .build()
        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        val result = sut.writeAnalytics(inputData)
        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun writeAnalytics_whenEventNameIsMissing_returnsFailure() {
        val inputData = Data.Builder()
            .putLong(AnalyticsClient.WORK_INPUT_KEY_TIMESTAMP, timestamp)
            .build()
        val sut =
            AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        val result = sut.writeAnalytics(inputData)
        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun writeAnalytics_whenTimestampIsMissing_returnsFailure() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_EVENT_NAME, eventName)
            .build()
        val sut =
            AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        val result = sut.writeAnalytics(inputData)
        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun writeAnalytics_addsEventToAnalyticsDatabaseAndReturnsSuccess() {
        val analyticsEventSlot = slot<AnalyticsEvent>()
        every { analyticsEventDao.insertEvent(capture(analyticsEventSlot)) } returns Unit

        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_EVENT_NAME, eventName)
            .putLong(AnalyticsClient.WORK_INPUT_KEY_TIMESTAMP, timestamp)
            .build()
        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        sut.writeAnalytics(inputData)

        val event = analyticsEventSlot.captured
        assertEquals("sample-event-name", event.name)
        assertEquals(123, event.timestamp)
    }

    @Test
    @Throws(Exception::class)
    fun uploadAnalytics_whenNoEventsExist_doesNothing() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration)
            .build()
        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        sut.uploadAnalytics(context, inputData)

        // or confirmVerified(httpClient)
        verify { httpClient wasNot Called }
    }

    @Test
    @Throws(Exception::class)
    fun uploadAnalytics_whenEventsExist_sendsAllEvents() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration)
            .build()
        val metadata = createSampleDeviceMetadata()

        every {
            deviceInspector.getDeviceMetadata(context, any(), sessionId, integration)
        } returns metadata

        val events: MutableList<AnalyticsEvent> = ArrayList()
        events.add(AnalyticsEvent("event0", 123))
        events.add(AnalyticsEvent("event1", 456))
        every { analyticsEventDao.getAllEvents() } returns events

        val analyticsJSONSlot = slot<String>()
        every { httpClient.post(any(), capture(analyticsJSONSlot), any(), any()) }

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        sut.uploadAnalytics(context, inputData)

        val analyticsJson = JSONObject(analyticsJSONSlot.captured)

        val eventsArray = analyticsJson.getJSONArray("events")
        val eventJSON = eventsArray[0] as JSONObject
        assertNotNull("JSON body missing top level `events` key.", eventJSON)

        val batchParams = eventJSON["batch_params"] as JSONObject
        assertEquals("fake-merchant-id", batchParams["merchant_id"])
        assertNotNull(batchParams)

        val eventParams = eventJSON.getJSONArray("event_params")
        assertEquals(2, eventParams.length())

        val eventOne = eventParams.getJSONObject(0)
        assertEquals("event0", eventOne.getString("event_name"))
        assertEquals(123, eventOne.getString("t").toLong())

        val eventTwo = eventParams.getJSONObject(1)
        assertEquals("event1", eventTwo.getString("event_name"))
        assertEquals(456, eventTwo.getString("t").toLong())
    }

    @Test
    fun uploadAnalytics_whenConfigurationIsNull_doesNothing() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration)
            .build()

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        val result = sut.uploadAnalytics(context, inputData)
        assertTrue(result is ListenableWorker.Result.Failure)

        // or confirmVerified(httpClient)
        verify { httpClient wasNot Called }
    }

    @Test
    @Throws(JSONException::class)
    fun uploadAnalytics_whenAuthorizationIsNull_doesNothing() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration)
            .build()

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        val result = sut.uploadAnalytics(context, inputData)
        assertTrue(result is ListenableWorker.Result.Failure)

        // or confirmVerified(httpClient)
        verify { httpClient wasNot Called }
    }

    @Test
    @Throws(JSONException::class)
    fun uploadAnalytics_whenSessionIdIsNull_doesNothing() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration)
            .build()

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        val result = sut.uploadAnalytics(context, inputData)
        assertTrue(result is ListenableWorker.Result.Failure)

        // or confirmVerified(httpClient)
        verify { httpClient wasNot Called }
    }

    @Test
    @Throws(JSONException::class)
    fun uploadAnalytics_whenIntegrationIsNull_doesNothing() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .build()

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        val result = sut.uploadAnalytics(context, inputData)
        assertTrue(result is ListenableWorker.Result.Failure)

        // or confirmVerified(httpClient)
        verify { httpClient wasNot Called }
    }

    @Test
    @Throws(Exception::class)
    fun uploadAnalytics_deletesDatabaseEventsOnSuccessResponse() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration)
            .build()

        val metadata = createSampleDeviceMetadata()
        every {
            deviceInspector.getDeviceMetadata(context, any(), sessionId, integration)
        } returns metadata

        val events: MutableList<AnalyticsEvent> = ArrayList()
        events.add(AnalyticsEvent("event0", 123))
        events.add(AnalyticsEvent("event1", 456))
        every { analyticsEventDao.getAllEvents() } returns events

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        sut.uploadAnalytics(context, inputData)

        verify { analyticsEventDao.deleteEvents(events) }
    }

    @Test
    @Throws(Exception::class)
    fun uploadAnalytics_whenAnalyticsSendFails_returnsError() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration)
            .build()

        every {
            deviceInspector.getDeviceMetadata(context, any(), sessionId, integration)
        } returns createSampleDeviceMetadata()

        val events: MutableList<AnalyticsEvent> = ArrayList()
        events.add(AnalyticsEvent("event0", 123))
        events.add(AnalyticsEvent("event1", 456))
        every { analyticsEventDao.getAllEvents() } returns events

        val httpError = Exception("error")
        every { httpClient.post(any(), any(), any(), any()) } throws httpError

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        val result = sut.uploadAnalytics(context, inputData)
        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    @Throws(Exception::class)
    fun reportCrash_sendsCrashAnalyticsEvent() {
        val metadata = createSampleDeviceMetadata()
        every {
            deviceInspector.getDeviceMetadata(context, configuration, sessionId, integration)
        } returns metadata

        val analyticsJSONSlot = slot<String>()
        every {
            httpClient.post(
                "https://api-m.paypal.com/v1/tracking/batch/events",
                capture(analyticsJSONSlot),
                any(),
                authorization,
                any()
            )
        } returns Unit

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        sut.sendEvent(configuration, eventName, sessionId, integration, authorization)

        sut.reportCrash(context, configuration, sessionId, integration, 123, authorization)

        val analyticsJson = JSONObject(analyticsJSONSlot.captured)

        val eventsArray = analyticsJson.getJSONArray("events")
        val eventJSON = eventsArray[0] as JSONObject
        assertNotNull("JSON body missing top level `events` key.", eventJSON)

        val batchParams = eventJSON["batch_params"] as JSONObject
        assertEquals("fake-merchant-id", batchParams["merchant_id"])
        assertNotNull(batchParams)

        val eventParams = eventJSON.getJSONArray("event_params")
        assertEquals(1, eventParams.length())

        val eventOne = eventParams.getJSONObject(0)
        assertEquals("android.crash", eventOne.getString("event_name"))
        assertEquals(123, eventOne.getString("t").toLong())
    }

    @Test
    @Throws(JSONException::class)
    fun reportCrash_whenAuthorizationIsNull_doesNothing() {
        val metadata = createSampleDeviceMetadata()
        every {
            deviceInspector.getDeviceMetadata(context, configuration, sessionId, integration)
        } returns metadata

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        sut.sendEvent(configuration, eventName, sessionId, integration, authorization)

        sut.reportCrash(context, configuration, sessionId, integration, 123, null)

        // or confirmVerified(httpClient)
        verify { httpClient wasNot Called }
    }

    companion object {
        private fun createSampleDeviceMetadata() = DeviceMetadata(
            appId = "fake-merchant-app-id",
            appName = "fake-merchant-app-name",
            clientSDKVersion = "fake-sdk-version",
            clientOs = "fake-os",
            deviceManufacturer = "fake-device-manufacturer",
            deviceModel = "fake-device-model",
            environment = "fake-environment",
            integrationType = "fake-integration",
            isSimulator = false,
            merchantAppVersion = "fake-merchant-app-version",
            merchantId = "fake-merchant-id",
            platform = "fake-platform",
            sessionId = "fake-session-id"
        )
    }
}
