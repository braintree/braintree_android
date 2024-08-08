package com.braintreepayments.api.core

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("SwallowedException", "TooGenericExceptionCaught")
internal class AnalyticsClient(
    context: Context,
    private val httpClient: BraintreeHttpClient = BraintreeHttpClient(),
    private val analyticsDatabase: AnalyticsDatabase = AnalyticsDatabase.getInstance(context.applicationContext),
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext),
    private val deviceInspector: DeviceInspector = DeviceInspector()
) {
    private val applicationContext = context.applicationContext

    fun sendEvent(
        configuration: Configuration,
        event: AnalyticsEvent,
        sessionId: String?,
        integration: IntegrationType?,
        authorization: Authorization
    ): UUID {
        scheduleAnalyticsWriteInBackground(event, authorization)
        return scheduleAnalyticsUploadInBackground(
            configuration,
            authorization,
            sessionId,
            integration
        )
    }

    private fun scheduleAnalyticsWriteInBackground(
        event: AnalyticsEvent, authorization: Authorization
    ) {
        val json = mapAnalyticsEventToFPTIEventJSON(event)
        val inputData = Data.Builder()
            .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(WORK_INPUT_KEY_ANALYTICS_JSON, json)
            .build()

        val analyticsWorkRequest =
            OneTimeWorkRequest.Builder(AnalyticsWriteToDbWorker::class.java)
                .setInputData(inputData)
                .build()
        workManager.enqueueUniqueWork(
            WORK_NAME_ANALYTICS_WRITE, ExistingWorkPolicy.APPEND_OR_REPLACE, analyticsWorkRequest
        )
    }

    fun performAnalyticsWrite(inputData: Data): ListenableWorker.Result {
        val analyticsJSON = inputData.getString(WORK_INPUT_KEY_ANALYTICS_JSON)
        return if (analyticsJSON == null) {
            ListenableWorker.Result.failure()
        } else {
            val eventBlob = AnalyticsEventBlob(analyticsJSON)
            val analyticsBlobDao = analyticsDatabase.analyticsEventBlobDao()
            analyticsBlobDao.insertEventBlob(eventBlob)
            ListenableWorker.Result.success()
        }
    }

    private fun scheduleAnalyticsUploadInBackground(
        configuration: Configuration,
        authorization: Authorization,
        sessionId: String?,
        integration: IntegrationType?
    ): UUID {
        val inputData = Data.Builder()
            .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(WORK_INPUT_KEY_INTEGRATION, integration?.stringValue)
            .build()

        val analyticsWorkRequest = OneTimeWorkRequest.Builder(AnalyticsUploadWorker::class.java)
            .setInitialDelay(DELAY_TIME_SECONDS, TimeUnit.SECONDS)
            .setInputData(inputData)
            .build()
        workManager.enqueueUniqueWork(
            WORK_NAME_ANALYTICS_UPLOAD, ExistingWorkPolicy.KEEP, analyticsWorkRequest
        )
        return analyticsWorkRequest.id
    }

    fun performAnalyticsUpload(inputData: Data): ListenableWorker.Result {
        val configuration = getConfigurationFromData(inputData)
        val authorization = getAuthorizationFromData(inputData)
        val sessionId = inputData.getString(WORK_INPUT_KEY_SESSION_ID)
        val integration = inputData.getString(WORK_INPUT_KEY_INTEGRATION)
        val isMissingInputData =
            listOf(configuration, authorization, sessionId, integration).contains(null)
        return if (isMissingInputData) {
            ListenableWorker.Result.failure()
        } else {
            try {
                val analyticsEventBlobDao = analyticsDatabase.analyticsEventBlobDao()
                val eventBlobs = analyticsEventBlobDao.getAllEventBlobs()
                if (eventBlobs.isNotEmpty()) {
                    val metadata = deviceInspector.getDeviceMetadata(
                        applicationContext,
                        configuration,
                        sessionId,
                        IntegrationType.fromString(integration)
                    )
                    val analyticsRequest = createFPTIPayload(authorization, eventBlobs, metadata)
                    httpClient.post(
                        FPTI_ANALYTICS_URL,
                        analyticsRequest.toString(),
                        configuration,
                        authorization
                    )
                    analyticsEventBlobDao.deleteEventBlobs(eventBlobs)
                }
                ListenableWorker.Result.success()
            } catch (e: Exception) {
                ListenableWorker.Result.failure()
            }
        }
    }

    fun reportCrash(
        context: Context?,
        configuration: Configuration?,
        sessionId: String?,
        integration: IntegrationType?,
        authorization: Authorization?
    ) {
        reportCrash(
            context,
            configuration,
            sessionId,
            integration,
            System.currentTimeMillis(),
            authorization
        )
    }

    @VisibleForTesting
    fun reportCrash(
        context: Context?,
        configuration: Configuration?,
        sessionId: String?,
        integration: IntegrationType?,
        timestamp: Long,
        authorization: Authorization?
    ) {
        if (authorization == null) {
            return
        }
        val metadata =
            deviceInspector.getDeviceMetadata(context, configuration, sessionId, integration)
        val event = AnalyticsEvent(name = "crash", timestamp = timestamp)
        val eventJSON = mapAnalyticsEventToFPTIEventJSON(event)
        val eventBlobs = listOf(AnalyticsEventBlob(eventJSON))
        try {
            val analyticsRequest = createFPTIPayload(authorization, eventBlobs, metadata)
            httpClient.post(
                path = FPTI_ANALYTICS_URL,
                data = analyticsRequest.toString(),
                configuration = null,
                authorization = authorization,
                callback = null
            )
        } catch (e: JSONException) { /* ignored */
        }
    }

    @Throws(JSONException::class)
    private fun createFPTIPayload(
        authorization: Authorization?,
        eventBlobs: List<AnalyticsEventBlob>,
        metadata: DeviceMetadata
    ): JSONObject {
        val batchParamsJSON = mapDeviceMetadataToFPTIBatchParamsJSON(metadata)
        authorization?.let {
            if (it is ClientToken) {
                batchParamsJSON.put(FPTI_KEY_AUTH_FINGERPRINT, it.bearer)
            } else {
                batchParamsJSON.put(FPTI_KEY_TOKENIZATION_KEY, it.bearer)
            }
        }

        val eventsContainerJSON = JSONObject()
        eventsContainerJSON.put(FPTI_KEY_BATCH_PARAMS, batchParamsJSON)

        val eventParamsJSON = JSONArray()
        for (blob in eventBlobs) {
            eventParamsJSON.put(JSONObject(blob.jsonString))
        }
        eventsContainerJSON.put(FPTI_KEY_EVENT_PARAMS, eventParamsJSON)

        // Single-element "events" array required by FPTI formatting
        val eventsArray = JSONArray(arrayOf(eventsContainerJSON))
        return JSONObject().put(FPTI_KEY_EVENTS, eventsArray)
    }

    @Throws(JSONException::class)
    private fun mapDeviceMetadataToFPTIBatchParamsJSON(metadata: DeviceMetadata): JSONObject {
        val isVenmoInstalled = deviceInspector.isVenmoInstalled(applicationContext)
        return metadata.run {
            JSONObject()
                .put(APP_ID_KEY, appId)
                .put(APP_NAME_KEY, appName)
                .put(CLIENT_SDK_VERSION_KEY, clientSDKVersion)
                .put(CLIENT_OS_KEY, clientOs)
                .put(COMPONENT_KEY, component)
                .put(DEVICE_MANUFACTURER_KEY, deviceManufacturer)
                .put(DEVICE_MODEL_KEY, deviceModel)
                .put(DROP_IN_SDK_VERSION, dropInSDKVersion)
                .put(EVENT_SOURCE_KEY, eventSource)
                .put(ENVIRONMENT_KEY, environment)
                .put(INTEGRATION_TYPE_KEY, integrationType?.stringValue)
                .put(IS_SIMULATOR_KEY, isSimulator)
                .put(MERCHANT_APP_VERSION_KEY, merchantAppVersion)
                .put(MERCHANT_ID_KEY, merchantId)
                .put(PLATFORM_KEY, platform)
                .put(SESSION_ID_KEY, sessionId)
                .put(FPTI_KEY_VENMO_INSTALLED, isVenmoInstalled)
        }
    }

    private fun mapAnalyticsEventToFPTIEventJSON(event: AnalyticsEvent): String {
        val json = JSONObject()
            .put(FPTI_KEY_EVENT_NAME, event.name)
            .put(FPTI_KEY_TIMESTAMP, event.timestamp)
            .put(FPTI_KEY_IS_VAULT, event.isVaultRequest)
            .put(FPTI_KEY_TENANT_NAME, "Braintree")
            .putOpt(FPTI_KEY_PAYPAL_CONTEXT_ID, event.payPalContextId)
            .putOpt(FPTI_KEY_LINK_TYPE, event.linkType)
            .putOpt(FPTI_KEY_START_TIME, event.startTime)
            .putOpt(FPTI_KEY_END_TIME, event.endTime)
            .putOpt(FPTI_KEY_ENDPOINT, event.endpoint)
        return json.toString()
    }

    companion object {
        private const val FPTI_ANALYTICS_URL = "https://api-m.paypal.com/v1/tracking/batch/events"

        private const val FPTI_KEY_PAYPAL_CONTEXT_ID = "paypal_context_id"
        private const val FPTI_KEY_VENMO_INSTALLED = "venmo_installed"
        private const val FPTI_KEY_IS_VAULT = "is_vault"
        private const val FPTI_KEY_LINK_TYPE = "link_type"
        private const val FPTI_KEY_TOKENIZATION_KEY = "tokenization_key"
        private const val FPTI_KEY_AUTH_FINGERPRINT = "authorization_fingerprint"
        private const val FPTI_KEY_EVENTS = "events"
        private const val FPTI_KEY_BATCH_PARAMS = "batch_params"
        private const val FPTI_KEY_EVENT_PARAMS = "event_params"
        private const val FPTI_KEY_EVENT_NAME = "event_name"
        private const val FPTI_KEY_TIMESTAMP = "t"
        private const val FPTI_KEY_TENANT_NAME = "tenant_name"
        private const val FPTI_KEY_START_TIME = "start_time"
        private const val FPTI_KEY_END_TIME = "end_time"
        private const val FPTI_KEY_ENDPOINT = "endpoint"

        private const val APP_ID_KEY = "app_id"
        private const val APP_NAME_KEY = "app_name"
        private const val CLIENT_SDK_VERSION_KEY = "c_sdk_ver"
        private const val CLIENT_OS_KEY = "client_os"
        private const val COMPONENT_KEY = "comp"
        private const val DEVICE_MANUFACTURER_KEY = "device_manufacturer"
        private const val DEVICE_MODEL_KEY = "mobile_device_model"
        private const val DROP_IN_SDK_VERSION = "drop_in_sdk_ver"
        private const val EVENT_SOURCE_KEY = "event_source"
        private const val ENVIRONMENT_KEY = "merchant_sdk_env"
        private const val INTEGRATION_TYPE_KEY = "api_integration_type"
        private const val IS_SIMULATOR_KEY = "is_simulator"
        private const val MERCHANT_APP_VERSION_KEY = "mapv"
        private const val MERCHANT_ID_KEY = "merchant_id"
        private const val PLATFORM_KEY = "platform"
        private const val SESSION_ID_KEY = "session_id"

        const val WORK_NAME_ANALYTICS_UPLOAD = "uploadAnalytics"
        const val WORK_NAME_ANALYTICS_WRITE = "writeAnalyticsToDb"

        const val WORK_INPUT_KEY_AUTHORIZATION = "authorization"
        const val WORK_INPUT_KEY_CONFIGURATION = "configuration"
        const val WORK_INPUT_KEY_INTEGRATION = "integration"
        const val WORK_INPUT_KEY_SESSION_ID = "sessionId"
        const val WORK_INPUT_KEY_ANALYTICS_JSON = "analyticsJson"

        private const val DELAY_TIME_SECONDS = 30L

        private fun getAuthorizationFromData(inputData: Data?): Authorization? =
            inputData?.getString(WORK_INPUT_KEY_AUTHORIZATION)?.let {
                Authorization.fromString(it)
            }

        private fun getConfigurationFromData(inputData: Data?): Configuration? =
            inputData?.getString(WORK_INPUT_KEY_CONFIGURATION)?.let {
                try {
                    Configuration.fromJson(it)
                } catch (ignored: JSONException) {
                    null
                }
            }
    }
}
