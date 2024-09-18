package com.braintreepayments.api.core

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.braintreepayments.api.sharedutils.Time
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
    private val deviceInspector: DeviceInspector = DeviceInspector(),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val time: Time = Time()
) {
    private val applicationContext = context.applicationContext

    fun sendEvent(
        configuration: Configuration,
        event: AnalyticsEvent,
        integration: IntegrationType?,
        authorization: Authorization
    ): UUID {
        scheduleAnalyticsWriteInBackground(event, authorization)
        return scheduleAnalyticsUploadInBackground(
            configuration,
            authorization,
            integration
        )
    }

    private fun scheduleAnalyticsWriteInBackground(
        event: AnalyticsEvent,
        authorization: Authorization
    ) {
        val json = mapAnalyticsEventToFPTIEventJSON(event)
        val inputData = Data.Builder()
            .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(WORK_INPUT_KEY_ANALYTICS_JSON, json)
            .putString(WORK_INPUT_KEY_SESSION_ID, analyticsParamRepository.sessionId)
            .build()

        val analyticsWorkRequest = OneTimeWorkRequest.Builder(AnalyticsWriteToDbWorker::class.java)
            .setInputData(inputData)
            .build()
        workManager.enqueueUniqueWork(
            WORK_NAME_ANALYTICS_WRITE, ExistingWorkPolicy.APPEND_OR_REPLACE, analyticsWorkRequest
        )
    }

    fun performAnalyticsWrite(inputData: Data): ListenableWorker.Result {
        val analyticsJSON = inputData.getString(WORK_INPUT_KEY_ANALYTICS_JSON)
        val sessionId = inputData.getString(WORK_INPUT_KEY_SESSION_ID)
        return if (analyticsJSON == null || sessionId == null) {
            ListenableWorker.Result.failure()
        } else {
            val eventBlob = AnalyticsEventBlob(
                jsonString = analyticsJSON,
                sessionId = sessionId
            )
            val analyticsBlobDao = analyticsDatabase.analyticsEventBlobDao()
            analyticsBlobDao.insertEventBlob(eventBlob)
            ListenableWorker.Result.success()
        }
    }

    private fun scheduleAnalyticsUploadInBackground(
        configuration: Configuration,
        authorization: Authorization,
        integration: IntegrationType?
    ): UUID {
        val sessionId = analyticsParamRepository.sessionId
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
            WORK_NAME_ANALYTICS_UPLOAD + sessionId,
            ExistingWorkPolicy.KEEP,
            analyticsWorkRequest
        )
        return analyticsWorkRequest.id
    }

    fun performAnalyticsUpload(inputData: Data): ListenableWorker.Result {
        val configuration = getConfigurationFromData(inputData)
        val authorization = getAuthorizationFromData(inputData)
        val sessionId = inputData.getString(WORK_INPUT_KEY_SESSION_ID)
        val integration = inputData.getString(WORK_INPUT_KEY_INTEGRATION)
        return when (null) {
            configuration, authorization, sessionId, integration -> {
                ListenableWorker.Result.failure()
            }

            else -> {
                try {
                    val analyticsEventBlobDao = analyticsDatabase.analyticsEventBlobDao()
                    val eventBlobs = analyticsEventBlobDao.getBlobsBySessionId(sessionId)
                    if (eventBlobs.isNotEmpty()) {
                        val metadata = deviceInspector.getDeviceMetadata(
                            applicationContext,
                            configuration,
                            sessionId,
                            IntegrationType.fromString(integration)
                        )
                        val analyticsRequest =
                            createFPTIPayload(authorization, eventBlobs, metadata)
                        
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
    }

    @VisibleForTesting
    fun reportCrash(
        context: Context?,
        configuration: Configuration?,
        integration: IntegrationType?,
        authorization: Authorization?
    ) {
        if (authorization == null) {
            return
        }
        val metadata = deviceInspector.getDeviceMetadata(
            context = context,
            configuration = configuration,
            sessionId = analyticsParamRepository.sessionId,
            integration = integration
        )
        val event = AnalyticsEvent(
            name = "crash",
            timestamp = time.currentTime
        )
        val eventJSON = mapAnalyticsEventToFPTIEventJSON(event)
        val eventBlobs = listOf(
            AnalyticsEventBlob(
                jsonString = eventJSON,
                sessionId = ""
            )
        )
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

    @Throws(JSONException::class)
    private fun mapDeviceMetadataToFPTIBatchParamsJSON(metadata: DeviceMetadata): JSONObject {
        val isVenmoInstalled = deviceInspector.isVenmoInstalled(applicationContext)
        return metadata.run {
            JSONObject()
                .put(FPTI_BATCH_KEY_APP_ID, appId)
                .put(FPTI_BATCH_KEY_APP_NAME, appName)
                .put(FPTI_BATCH_KEY_CLIENT_SDK_VERSION, clientSDKVersion)
                .put(FPTI_BATCH_KEY_CLIENT_OS, clientOs)
                .put(FPTI_BATCH_KEY_COMPONENT, component)
                .put(FPTI_BATCH_KEY_DEVICE_MANUFACTURER, deviceManufacturer)
                .put(FPTI_BATCH_KEY_DEVICE_MODEL, deviceModel)
                .put(FPTI_BATCH_KEY_DROP_IN_SDK_VERSION, dropInSDKVersion)
                .put(FPTI_BATCH_KEY_EVENT_SOURCE, eventSource)
                .put(FPTI_BATCH_KEY_ENVIRONMENT, environment)
                .put(FPTI_BATCH_KEY_INTEGRATION_TYPE, integrationType?.stringValue)
                .put(FPTI_BATCH_KEY_IS_SIMULATOR, isSimulator)
                .put(FPTI_BATCH_KEY_MERCHANT_APP_VERSION, merchantAppVersion)
                .put(FPTI_BATCH_KEY_MERCHANT_ID, merchantId)
                .put(FPTI_BATCH_KEY_PLATFORM, platform)
                .put(FPTI_BATCH_KEY_SESSION_ID, sessionId)
                .put(FPTI_BATCH_KEY_VENMO_INSTALLED, isVenmoInstalled)
        }
    }

    companion object {
        private const val FPTI_ANALYTICS_URL = "https://api-m.paypal.com/v1/tracking/batch/events"

        private const val FPTI_KEY_PAYPAL_CONTEXT_ID = "paypal_context_id"
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

        private const val FPTI_BATCH_KEY_VENMO_INSTALLED = "venmo_installed"
        private const val FPTI_BATCH_KEY_APP_ID = "app_id"
        private const val FPTI_BATCH_KEY_APP_NAME = "app_name"
        private const val FPTI_BATCH_KEY_CLIENT_SDK_VERSION = "c_sdk_ver"
        private const val FPTI_BATCH_KEY_CLIENT_OS = "client_os"
        private const val FPTI_BATCH_KEY_COMPONENT = "comp"
        private const val FPTI_BATCH_KEY_DEVICE_MANUFACTURER = "device_manufacturer"
        private const val FPTI_BATCH_KEY_DEVICE_MODEL = "mobile_device_model"
        private const val FPTI_BATCH_KEY_DROP_IN_SDK_VERSION = "drop_in_sdk_ver"
        private const val FPTI_BATCH_KEY_EVENT_SOURCE = "event_source"
        private const val FPTI_BATCH_KEY_ENVIRONMENT = "merchant_sdk_env"
        private const val FPTI_BATCH_KEY_INTEGRATION_TYPE = "api_integration_type"
        private const val FPTI_BATCH_KEY_IS_SIMULATOR = "is_simulator"
        private const val FPTI_BATCH_KEY_MERCHANT_APP_VERSION = "mapv"
        private const val FPTI_BATCH_KEY_MERCHANT_ID = "merchant_id"
        private const val FPTI_BATCH_KEY_PLATFORM = "platform"
        private const val FPTI_BATCH_KEY_SESSION_ID = "session_id"

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
