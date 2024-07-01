package com.braintreepayments.api

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
internal class AnalyticsClient @VisibleForTesting constructor(
    private val applicationContext: Context,
    private val httpClient: BraintreeHttpClient,
    private val analyticsDatabase: AnalyticsDatabase,
    private val workManager: WorkManager,
    private val deviceInspector: DeviceInspector
) {
    constructor(context: Context) : this(
        context.applicationContext,
        BraintreeHttpClient(),
        AnalyticsDatabase.getInstance(context.applicationContext),
        WorkManager.getInstance(context.applicationContext),
        DeviceInspector()
    )

    fun sendEvent(
        configuration: Configuration,
        event: AnalyticsEvent,
        sessionId: String?,
        integration: String?,
        authorization: Authorization
    ): UUID {
        scheduleAnalyticsWrite(event, authorization)
        return scheduleAnalyticsUpload(configuration, authorization, sessionId, integration)
    }

    private fun scheduleAnalyticsWrite(
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

    fun writeAnalytics(inputData: Data): ListenableWorker.Result {
        val analyticsJSON = inputData.getString(WORK_INPUT_KEY_ANALYTICS_JSON)
        return if (analyticsJSON == null) {
            ListenableWorker.Result.failure()
        } else {
            val eventBlob = AnalyticsEventBlob(analyticsJSON)
            val analyticsBlobDao = analyticsDatabase.analyticsEventBlobDao()
            analyticsBlobDao.insertBlob(eventBlob)
            ListenableWorker.Result.success()
        }
    }

    private fun scheduleAnalyticsUpload(
        configuration: Configuration,
        authorization: Authorization,
        sessionId: String?,
        integration: String?
    ): UUID {
        val inputData = Data.Builder()
            .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
            .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
            .putString(WORK_INPUT_KEY_INTEGRATION, integration)
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

    fun uploadAnalytics(inputData: Data): ListenableWorker.Result {
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
                val analyticsBlobDao = analyticsDatabase.analyticsEventBlobDao()
                val blobs = analyticsBlobDao.getAllBlobs()
                if (blobs.isNotEmpty()) {
                    val analyticsRequest =
                        createFPTIPayload(blobs, authorization, configuration, sessionId, integration)
                    httpClient.post(
                        FPTI_ANALYTICS_URL,
                        analyticsRequest.toString(),
                        configuration,
                        authorization
                    )
                    analyticsBlobDao.deleteBlobs(blobs)
                }
                ListenableWorker.Result.success()
            } catch (e: Exception) {
                ListenableWorker.Result.failure()
            }
        }
    }

    fun reportCrash(
        configuration: Configuration?,
        sessionId: String?,
        integration: String?,
        authorization: Authorization?
    ) {
        reportCrash(
            configuration,
            sessionId,
            integration,
            System.currentTimeMillis(),
            authorization
        )
    }

    @VisibleForTesting
    fun reportCrash(
        configuration: Configuration?,
        sessionId: String?,
        integration: String?,
        timestamp: Long,
        authorization: Authorization?
    ) {
        if (authorization == null) {
            return
        }
        val event = AnalyticsEvent(name = "crash", timestamp = timestamp)
        val eventJSON = mapAnalyticsEventToFPTIEventJSON(event)
        val eventBlob = AnalyticsEventBlob(eventJSON)
        val blobs = listOf(eventBlob)
        try {
            val analyticsRequest =
                createFPTIPayload(blobs, authorization, configuration, sessionId, integration)
            httpClient.post(
                path = FPTI_ANALYTICS_URL,
                data = analyticsRequest.toString(),
                configuration = null,
                authorization = authorization,
                callback = HttpNoResponse()
            )
        } catch (e: JSONException) { /* ignored */
        }
    }

    @Throws(JSONException::class)
    private fun createFPTIPayload(
        blobs: List<AnalyticsEventBlob>,
        authorization: Authorization?,
        configuration: Configuration?,
        sessionId: String?,
        integrationType: String?
    ): JSONObject {
        val metadata = deviceInspector.getDeviceMetadata(
            applicationContext,
            configuration,
            sessionId,
            integrationType
        )
        val batchParamsJSON = mapDeviceMetadataToFPTIBatchJSON(metadata)
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
        for (analyticsBlob in blobs) {
            eventParamsJSON.put(JSONObject(analyticsBlob.json))
        }
        eventsContainerJSON.put(FPTI_KEY_EVENT_PARAMS, eventParamsJSON)

        // Single-element "events" array required by FPTI formatting
        val eventsArray = JSONArray(arrayOf(eventsContainerJSON))
        return JSONObject().put(FPTI_KEY_EVENTS, eventsArray)
    }

    private fun mapAnalyticsEventToFPTIEventJSON(event: AnalyticsEvent): String {
        val json = JSONObject()
            .put(FPTI_KEY_EVENT_NAME, "android.${event.name}")
            .put(FPTI_KEY_TIMESTAMP, event.timestamp)
            .put(FPTI_KEY_VENMO_INSTALLED, event.venmoInstalled)
            .put(FPTI_KEY_IS_VAULT, event.isVaultRequest)
            .put(FPTI_KEY_TENANT_NAME, "Braintree")
            .putOpt(FPTI_KEY_PAYPAL_CONTEXT_ID, event.payPalContextId)
            .putOpt(FPTI_KEY_LINK_TYPE, event.linkType)
        return json.toString()
    }

    @Throws(JSONException::class)
    fun mapDeviceMetadataToFPTIBatchJSON(deviceMetadata: DeviceMetadata): JSONObject {
        return JSONObject()
            .put(BATCH_KEY_APP_ID, deviceMetadata.appId)
            .put(BATCH_KEY_APP_NAME, deviceMetadata.appName)
            .put(BATCH_KEY_CLIENT_SDK_VERSION, deviceMetadata.clientSDKVersion)
            .put(BATCH_KEY_CLIENT_OS, deviceMetadata.clientOs)
            .put(BATCH_KEY_COMPONENT, deviceMetadata.component)
            .put(BATCH_KEY_DEVICE_MANUFACTURER, deviceMetadata.deviceManufacturer)
            .put(BATCH_KEY_DEVICE_MODEL, deviceMetadata.deviceModel)
            .put(BATCH_KEY_DROP_IN_SDK_VERSION, deviceMetadata.dropInSDKVersion)
            .put(BATCH_KEY_EVENT_SOURCE, deviceMetadata.eventSource)
            .put(BATCH_KEY_ENVIRONMENT, deviceMetadata.environment)
            .put(BATCH_KEY_INTEGRATION_TYPE, deviceMetadata.integrationType)
            .put(BATCH_KEY_IS_SIMULATOR, deviceMetadata.isSimulator)
            .put(BATCH_KEY_MERCHANT_APP_VERSION, deviceMetadata.merchantAppVersion)
            .put(BATCH_KEY_MERCHANT_ID, deviceMetadata.merchantId)
            .put(BATCH_KEY_PLATFORM, deviceMetadata.platform)
            .put(BATCH_KEY_SESSION_ID, deviceMetadata.sessionId)
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

        private const val BATCH_KEY_APP_ID = "app_id"
        private const val BATCH_KEY_APP_NAME = "app_name"
        private const val BATCH_KEY_CLIENT_SDK_VERSION = "c_sdk_ver"
        private const val BATCH_KEY_CLIENT_OS = "client_os"
        private const val BATCH_KEY_COMPONENT = "comp"
        private const val BATCH_KEY_DEVICE_MANUFACTURER = "device_manufacturer"
        private const val BATCH_KEY_DEVICE_MODEL = "mobile_device_model"
        private const val BATCH_KEY_DROP_IN_SDK_VERSION = "drop_in_sdk_ver"
        private const val BATCH_KEY_EVENT_SOURCE = "event_source"
        private const val BATCH_KEY_ENVIRONMENT = "merchant_sdk_env"
        private const val BATCH_KEY_INTEGRATION_TYPE = "api_integration_type"
        private const val BATCH_KEY_IS_SIMULATOR = "is_simulator"
        private const val BATCH_KEY_MERCHANT_APP_VERSION = "mapv"
        private const val BATCH_KEY_MERCHANT_ID = "merchant_id"
        private const val BATCH_KEY_PLATFORM = "platform"
        private const val BATCH_KEY_SESSION_ID = "session_id"

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
