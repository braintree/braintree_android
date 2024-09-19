package com.braintreepayments.api.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.*
import com.braintreepayments.api.core.AnalyticsClient.Companion.WORK_INPUT_KEY_ANALYTICS_JSON
import com.braintreepayments.api.core.AnalyticsClient.Companion.WORK_INPUT_KEY_SESSION_ID
import com.braintreepayments.api.core.AnalyticsClient.Companion.WORK_NAME_ANALYTICS_UPLOAD
import com.braintreepayments.api.core.Authorization.Companion.fromString
import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.sharedutils.Time
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.*
import org.json.JSONException
import org.json.JSONObject
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
    private lateinit var analyticsParamRepository: AnalyticsParamRepository
    private lateinit var time: Time
    private lateinit var eventName: String
    private lateinit var sessionId: String
    private lateinit var payPalContextId: String
    private lateinit var linkType: String
    private lateinit var integration: IntegrationType
    private lateinit var workManager: WorkManager
    private lateinit var analyticsDatabase: AnalyticsDatabase
    private lateinit var analyticsEventBlobDao: AnalyticsEventBlobDao

    private lateinit var sut: AnalyticsClient

    private var timestamp: Long = 0

    @Before
    @Throws(InvalidArgumentException::class, GeneralSecurityException::class, IOException::class)
    fun beforeEach() {
        timestamp = 123
        eventName = "sample-event-name"
        sessionId = "sample-session-id"
        payPalContextId = "sample-paypal-context-id"
        linkType = "sample-link-type"
        integration = IntegrationType.CUSTOM
        authorization = fromString(Fixtures.TOKENIZATION_KEY)
        context = ApplicationProvider.getApplicationContext()
        configuration = fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        httpClient = mockk(relaxed = true)
        deviceInspector = mockk(relaxed = true)
        analyticsParamRepository = mockk(relaxed = true)
        analyticsDatabase = mockk(relaxed = true)
        analyticsEventBlobDao = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        time = mockk(relaxed = true)

        every { analyticsDatabase.analyticsEventBlobDao() } returns analyticsEventBlobDao
        every { analyticsParamRepository.sessionId } returns sessionId

        every { time.currentTime } returns 123

        sut = AnalyticsClient(
            context = context,
            httpClient = httpClient,
            analyticsDatabase = analyticsDatabase,
            workManager = workManager,
            deviceInspector = deviceInspector,
            analyticsParamRepository = analyticsParamRepository,
            time = time
        )
    }

    @Test
    @Throws(JSONException::class)
    fun sendEvent_convertsAnalyticsEventWithRequiredParamsToJSONAndEnqueuesItForWriteToDbWorker() {
        val workRequestSlot = slot<OneTimeWorkRequest>()
        every {
            workManager.enqueueUniqueWork(
                "writeAnalyticsToDb",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                capture(workRequestSlot)
            )
        } returns mockk()

        val event = AnalyticsEvent(eventName, timestamp = 123)

        sut.sendEvent(configuration, event, integration, authorization)

        val workSpec = workRequestSlot.captured.workSpec
        assertEquals(AnalyticsWriteToDbWorker::class.java.name, workSpec.workerClassName)
        assertEquals(authorization.toString(), workSpec.input.getString("authorization"))

        // language=JSON
        val expectedJSON = """
        {
          "event_name": "sample-event-name",
          "t": 123,
          "is_vault": false,
          "tenant_name": "Braintree"
        }
        """
        val actualJSON = workSpec.input.getString(WORK_INPUT_KEY_ANALYTICS_JSON)!!
        JSONAssert.assertEquals(JSONObject(expectedJSON), JSONObject(actualJSON), true)
    }

    fun sendEvent_convertsAnalyticsEventWithOptionalParamsToJSONAndEnqueuesItForWriteToDbWorker() {
        val workRequestSlot = slot<OneTimeWorkRequest>()
        every {
            workManager.enqueueUniqueWork(
                "writeAnalyticsToDb",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                capture(workRequestSlot)
            )
        } returns mockk()

        val event = AnalyticsEvent(
            name = eventName,
            payPalContextId = "fake-paypal-context-id",
            linkType = "fake-link-type",
            isVaultRequest = true,
            timestamp = 456
        )

        sut.sendEvent(configuration, event, integration, authorization)

        val workSpec = workRequestSlot.captured.workSpec
        assertEquals(30000, workSpec.initialDelay)
        assertEquals(AnalyticsUploadWorker::class.java.name, workSpec.workerClassName)
        assertEquals(configuration.toJson(), workSpec.input.getString("configuration"))
        assertEquals(authorization.toString(), workSpec.input.getString("authorization"))
        assertEquals("sample-session-id", workSpec.input.getString("sessionId"))
        assertEquals("sample-integration", workSpec.input.getString("integration"))

        // language=JSON
        val expectedJSON = """
        {
          "event_name": "sample-event-name",
          "paypal_context_id": "fake-paypal-context-id",
          "link_type": "fake-link-type",
          "t": 456,
          "is_vault": true,
          "tenant_name": "Braintree",
          "start_time": 789,
          "end_time": 987,
          "endpoint": "fake-endpoint"
        }
        """
        val actualJSON = workSpec.input.getString(WORK_INPUT_KEY_ANALYTICS_JSON)!!
        JSONAssert.assertEquals(JSONObject(expectedJSON), JSONObject(actualJSON), true)
    }

    @Test
    fun writeAnalytics_whenAnalyticsJSONIsPresent_returnsSuccess() {
        val inputData = Data.Builder()
            .putString(WORK_INPUT_KEY_ANALYTICS_JSON, JSONObject().toString())
            .putString(WORK_INPUT_KEY_SESSION_ID, JSONObject().toString())
            .build()
        val result = sut.performAnalyticsWrite(inputData)
        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun writeAnalytics_whenAnalyticsJSONIsMissing_returnsSuccess() {
        val inputData = Data.Builder().build()
        val result = sut.performAnalyticsWrite(inputData)
        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun writeAnalytics_addsEventToAnalyticsDatabaseAndReturnsSuccess() {
        val analyticsEventBlobSlot = slot<AnalyticsEventBlob>()
        every { analyticsEventBlobDao.insertEventBlob(capture(analyticsEventBlobSlot)) } returns Unit

        val json = JSONObject().put("fake", "json").toString()
        val inputData = Data.Builder()
            .putString(WORK_INPUT_KEY_ANALYTICS_JSON, json)
            .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
            .build()

        sut.performAnalyticsWrite(inputData)

        val blob = analyticsEventBlobSlot.captured
        assertEquals(json, blob.jsonString)
    }

    @Test
    @Throws(Exception::class)
    fun uploadAnalytics_whenNoEventsExist_doesNothing() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration.stringValue)
            .build()
        val sut =
            AnalyticsClient(context, httpClient, analyticsDatabase, workManager, deviceInspector)
        sut.performAnalyticsUpload(inputData)

        // or confirmVerified(httpClient)
        verify { httpClient wasNot Called }
    }

    @Test
    @Throws(Exception::class)
    @Suppress("LongMethod")
    fun uploadAnalytics_whenEventsExist_sendsAllEvents() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration.stringValue)
            .build()
        val metadata = createSampleDeviceMetadata()

        every { deviceInspector.isVenmoInstalled(context) } returns true
        every { deviceInspector.isPayPalInstalled(context) } returns true
        every {
            deviceInspector.getDeviceMetadata(context, any(), sessionId, integration)
        } returns metadata

        val blobs = listOf(
            AnalyticsEventBlob(jsonString = """{ "fake": "json" }""", sessionId = sessionId)
        )
        every { analyticsEventBlobDao.getBlobsBySessionId(sessionId) } returns blobs

        val analyticsJSONSlot = slot<String>()
        every {
            httpClient.post(
                "https://api-m.paypal.com/v1/tracking/batch/events",
                capture(analyticsJSONSlot),
                any(),
                any()
            )
        }

        sut.performAnalyticsUpload(inputData)

        // language=JSON
        val expectedJSON = """
        {
          "events": [
            {
              "batch_params": {
                "app_id": "fake-app-id",
                "app_name": "fake-app-name",
                "c_sdk_ver": "fake-sdk-version",
                "client_os": "fake-os",
                "comp": "fake-component",
                "device_manufacturer": "fake-device-manufacturer",
                "mobile_device_model": "fake-mobile-device-model",
                "event_source": "fake-event-source",
                "merchant_sdk_env": "fake-environment",
                "api_integration_type": "custom",
                "is_simulator": false,
                "venmo_installed": true,
                "paypal_installed": true,
                "mapv": "fake-merchant-app-version",
                "merchant_id": "fake-merchant-id",
                "platform": "fake-platform",
                "session_id": "$sessionId",
                "tokenization_key": "sandbox_tmxhyf7d_dcpspy2brwdjr3qn"
              },
              "event_params": [
                { "fake": "json" }
              ]
            }
          ]
        }
        """
        val actualJSON = JSONObject(analyticsJSONSlot.captured)
        JSONAssert.assertEquals(JSONObject(expectedJSON), actualJSON, true)
    }

    @Test
    fun uploadAnalytics_whenConfigurationIsNull_doesNothing() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration.stringValue)
            .build()

        val result = sut.performAnalyticsUpload(inputData)
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
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration.stringValue)
            .build()

        val result = sut.performAnalyticsUpload(inputData)
        assertTrue(result is ListenableWorker.Result.Failure)

        // or confirmVerified(httpClient)
        verify { httpClient wasNot Called }
    }

    @Test
    @Throws(JSONException::class)
    fun uploadAnalytics_whenAuthorizationIsClientToken_includesAuthFingerprintBatchParam() {
        val inputData = Data.Builder()
            .putString(
                AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION,
                Fixtures.BASE64_CLIENT_TOKEN2
            )
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration.stringValue)
            .build()

        every {
            deviceInspector.getDeviceMetadata(context, any(), sessionId, integration)
        } returns createSampleDeviceMetadata()

        val blobs = listOf(
            AnalyticsEventBlob(
                jsonString = """{ "fake": "json" }""",
                sessionId = sessionId
            )
        )
        every { analyticsEventBlobDao.getBlobsBySessionId(sessionId) } returns blobs

        val analyticsJSONSlot = slot<String>()
        every { httpClient.post(any(), capture(analyticsJSONSlot), any(), any()) }

        sut.performAnalyticsUpload(inputData)

        val analyticsJson = JSONObject(analyticsJSONSlot.captured)

        val eventJSON = analyticsJson.getJSONArray("events")[0] as JSONObject
        val batchParams = eventJSON["batch_params"] as JSONObject
        assertEquals("encoded_auth_fingerprint", batchParams["authorization_fingerprint"])
    }

    @Test
    @Throws(JSONException::class)
    fun uploadAnalytics_whenSessionIdIsNull_doesNothing() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration.stringValue)
            .build()

        val result = sut.performAnalyticsUpload(inputData)
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

        val result = sut.performAnalyticsUpload(inputData)
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
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration.stringValue)
            .build()

        val metadata = createSampleDeviceMetadata()
        every {
            deviceInspector.getDeviceMetadata(context, any(), sessionId, integration)
        } returns metadata

        val blobs = listOf(
            AnalyticsEventBlob(
                jsonString = """{ "fake": "json" }""",
                sessionId = sessionId
            )
        )
        every { analyticsEventBlobDao.getBlobsBySessionId(sessionId) } returns blobs

        sut.performAnalyticsUpload(inputData)

        verify { analyticsEventBlobDao.deleteEventBlobs(blobs) }
    }

    @Test
    @Throws(Exception::class)
    fun uploadAnalytics_whenAnalyticsSendFails_returnsError() {
        val inputData = Data.Builder()
            .putString(AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(AnalyticsClient.WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(AnalyticsClient.WORK_INPUT_KEY_INTEGRATION, integration.stringValue)
            .build()

        every {
            deviceInspector.getDeviceMetadata(context, any(), sessionId, integration)
        } returns createSampleDeviceMetadata()

        val blobs = listOf(
            AnalyticsEventBlob(
                jsonString = """{ "fake": "json" }""",
                sessionId = sessionId
            )
        )
        every { analyticsEventBlobDao.getBlobsBySessionId(sessionId) } returns blobs

        val httpError = Exception("error")
        every { httpClient.post(any(), any(), any(), any()) } throws httpError

        val result = sut.performAnalyticsUpload(inputData)
        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    @Throws(Exception::class)
    @Suppress("LongMethod")
    fun reportCrash_sendsCrashAnalyticsEvent() {
        every { analyticsParamRepository.sessionId } returns sessionId
        every { deviceInspector.isVenmoInstalled(context) } returns false
        every { deviceInspector.isPayPalInstalled(context) } returns false
        every {
            deviceInspector.getDeviceMetadata(context, configuration, sessionId, integration)
        } returns createSampleDeviceMetadata()

        val analyticsJSONSlot = slot<String>()
        every {
            httpClient.post(
                path = "https://api-m.paypal.com/v1/tracking/batch/events",
                data = capture(analyticsJSONSlot),
                configuration = any(),
                authorization = authorization,
                callback = any()
            )
        } returns Unit

        sut.reportCrash(context, configuration, integration, authorization)

        // language=JSON
        val expectedJSON = """
        {
          "events": [
            {
              "batch_params": {
                "app_id": "fake-app-id",
                "app_name": "fake-app-name",
                "c_sdk_ver": "fake-sdk-version",
                "client_os": "fake-os",
                "comp": "fake-component",
                "device_manufacturer": "fake-device-manufacturer",
                "mobile_device_model": "fake-mobile-device-model",
                "event_source": "fake-event-source",
                "merchant_sdk_env": "fake-environment",
                "api_integration_type": "custom",
                "is_simulator": false,
                "venmo_installed": false,
                "paypal_installed": false,
                "mapv": "fake-merchant-app-version",
                "merchant_id": "fake-merchant-id",
                "platform": "fake-platform",
                "session_id": "$sessionId",
                "tokenization_key": "sandbox_tmxhyf7d_dcpspy2brwdjr3qn"
              },
              "event_params": [
                {
                    "event_name": "crash",
                    "t": 123,
                    "tenant_name": "Braintree",
                    "is_vault": false
                }
              ]
            }
          ]
        }
        """
        val actualJSON = JSONObject(analyticsJSONSlot.captured)
        JSONAssert.assertEquals(JSONObject(expectedJSON), actualJSON, true)
    }

    @Test
    @Throws(JSONException::class)
    fun reportCrash_whenAuthorizationIsNull_doesNothing() {
        val metadata = createSampleDeviceMetadata()
        every {
            deviceInspector.getDeviceMetadata(context, configuration, sessionId, integration)
        } returns metadata

        val event = AnalyticsEvent(eventName, timestamp)
        sut.sendEvent(configuration, event, integration, authorization)

        sut.reportCrash(context, configuration, integration, null)

        // or confirmVerified(httpClient)
        verify { httpClient wasNot Called }
    }

    @Test
    fun `sendEvent enqueues work to upload analytic events with sessionId in the name`() {
        sut.sendEvent(configuration, AnalyticsEvent("event-name", timestamp), integration, authorization)

        verify {
            workManager.enqueueUniqueWork(
                WORK_NAME_ANALYTICS_UPLOAD + sessionId,
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>()
            )
        }
    }

    companion object {
        private fun createSampleDeviceMetadata() = DeviceMetadata(
            appId = "fake-app-id",
            appName = "fake-app-name",
            clientSDKVersion = "fake-sdk-version",
            clientOs = "fake-os",
            component = "fake-component",
            deviceManufacturer = "fake-device-manufacturer",
            deviceModel = "fake-mobile-device-model",
            environment = "fake-environment",
            eventSource = "fake-event-source",
            integrationType = IntegrationType.CUSTOM,
            isSimulator = false,
            merchantAppVersion = "fake-merchant-app-version",
            merchantId = "fake-merchant-id",
            platform = "fake-platform",
            sessionId = "sample-session-id"
        )
    }
}
