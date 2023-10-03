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

        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
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

        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
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
        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
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
        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration)
            .build()
        val metadata = createSampleDeviceMetadata()

        every {
            deviceInspector.getDeviceMetadata(context, sessionId, integration)
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
        val meta = analyticsJson.getJSONObject("_meta")
        JSONAssert.assertEquals(metadata.toJSON(), meta, true)

        val array = analyticsJson.getJSONArray("analytics")
        assertEquals(2, array.length())

        val eventOne = array.getJSONObject(0)
        assertEquals("event0", eventOne.getString("kind"))
        assertEquals(123, eventOne.getString("timestamp").toLong())

        val eventTwo = array.getJSONObject(1)
        assertEquals("event1", eventTwo.getString("kind"))
        assertEquals(456, eventTwo.getString("timestamp").toLong())
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
        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
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
        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
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
        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
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
        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration)
            .build()

        val metadata = createSampleDeviceMetadata()
        every {
            deviceInspector.getDeviceMetadata(
                context,
                sessionId,
                integration
            )
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
        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration)
            .build()

        val metadata = createSampleDeviceMetadata()
        every {
            deviceInspector.getDeviceMetadata(context, sessionId, integration)
        } returns metadata

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
    fun reportCrash_whenLastKnownAnalyticsUrlExists_sendsCrashAnalyticsEvent() {
        val metadata = createSampleDeviceMetadata()
        every {
            deviceInspector.getDeviceMetadata(context, sessionId, integration)
        } returns metadata

        val analyticsJSONSlot = slot<String>()
        every {
            httpClient.post(
                "analytics_url",
                capture(analyticsJSONSlot),
                isNull(),
                authorization,
                any()
            )
        } returns Unit

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
        sut.sendEvent(configuration, eventName, sessionId, integration, authorization)

        sut.reportCrash(context, sessionId, integration, 123, authorization)

        val analyticsJson = JSONObject(analyticsJSONSlot.captured)
        val meta = analyticsJson.getJSONObject("_meta")
        JSONAssert.assertEquals(metadata.toJSON(), meta, true)

        val array = analyticsJson.getJSONArray("analytics")
        assertEquals(1, array.length())

        val eventOne = array.getJSONObject(0)
        assertEquals("android.crash", eventOne.getString("kind"))
        assertEquals(123, eventOne.getString("timestamp").toLong())
    }

    @Test
    @Throws(JSONException::class)
    fun reportCrash_whenLastKnownAnalyticsUrlMissing_doesNothing() {
        val metadata = createSampleDeviceMetadata()
        every {
            deviceInspector.getDeviceMetadata(context, sessionId, integration)
        } returns metadata

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        sut.reportCrash(context, sessionId, integration, 123, authorization)

        // or confirmVerified(httpClient)
        verify { httpClient wasNot Called }
    }

    @Test
    @Throws(JSONException::class)
    fun reportCrash_whenAuthorizationIsNull_doesNothing() {
        val metadata = createSampleDeviceMetadata()
        every {
            deviceInspector.getDeviceMetadata(context, sessionId, integration)
        } returns metadata

        val sut = AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector)
        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
        sut.sendEvent(configuration, eventName, sessionId, integration, authorization)

        sut.reportCrash(context, sessionId, integration, 123, null)

        // or confirmVerified(httpClient)
        verify { httpClient wasNot Called }
    }

    companion object {
        private fun createSampleDeviceMetadata() = DeviceMetadata(
                integration = "sample-integration",
                sessionId = "sample-session-id",
                platform = "platform",
                sdkVersion = "sdk-version",
                deviceManufacturer = "device-manufacturer",
                deviceModel = "device-model",
                platformVersion = "platform-version",
                merchantAppName = "merchant-app-name",
                devicePersistentUUID = "persistent-uuid",
                merchantAppId = "merchant-app-name",
                userOrientation = "user-orientation",
                isPayPalInstalled = true,
                isVenmoInstalled = true,
                isSimulator = false
            )
    }
}
