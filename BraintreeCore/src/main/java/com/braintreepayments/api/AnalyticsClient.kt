package com.braintreepayments.api

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.braintreepayments.api.AnalyticsDatabase.Companion.getInstance
import com.braintreepayments.api.sharedutils.HttpNoResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("SwallowedException", "TooGenericExceptionCaught")
internal class AnalyticsClient @VisibleForTesting constructor(
    private val httpClient: BraintreeHttpClient,
    private val analyticsDatabase: AnalyticsDatabase,
    private val workManager: WorkManager,
    private val deviceInspector: DeviceInspector
) {
    constructor(context: Context) : this(
        BraintreeHttpClient(),
        getInstance(context.applicationContext),
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
        val inputData = Data.Builder()
            .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(WORK_INPUT_KEY_EVENT_NAME, "android.${event.name}")
            .putLong(WORK_INPUT_KEY_TIMESTAMP, event.timestamp)
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
        val eventName = inputData.getString(WORK_INPUT_KEY_EVENT_NAME)
        val payPalContextId = inputData.getString(WORK_INPUT_KEY_PAYPAL_CONTEXT_ID)
        val timestamp = inputData.getLong(WORK_INPUT_KEY_TIMESTAMP, INVALID_TIMESTAMP)

        return if (eventName == null || timestamp == INVALID_TIMESTAMP) {
            ListenableWorker.Result.failure()
        } else {
            val event = AnalyticsEvent(eventName, payPalContextId, timestamp)
            val analyticsEventDao = analyticsDatabase.analyticsEventDao()
            analyticsEventDao.insertEvent(event)
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

    fun uploadAnalytics(context: Context?, inputData: Data): ListenableWorker.Result {
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
                val analyticsEventDao = analyticsDatabase.analyticsEventDao()
                val events = analyticsEventDao.getAllEvents()
                if (events.isNotEmpty()) {
                    val metadata = deviceInspector.getDeviceMetadata(context, configuration, sessionId, integration)
                    val analyticsRequest = serializeEvents(authorization, events, metadata)

                    httpClient.post(
                        FPTI_ANALYTICS_URL,
                        analyticsRequest.toString(),
                        configuration,
                        authorization
                    )
                    analyticsEventDao.deleteEvents(events)
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
        integration: String?,
        authorization: Authorization?
    ) {
        reportCrash(context, configuration, sessionId, integration, System.currentTimeMillis(), authorization)
    }

    @VisibleForTesting
    fun reportCrash(
        context: Context?,
        configuration: Configuration?,
        sessionId: String?,
        integration: String?,
        timestamp: Long,
        authorization: Authorization?
    ) {
        if (authorization == null) {
            return
        }
        val metadata = deviceInspector.getDeviceMetadata(context, configuration, sessionId, integration)
        val event = AnalyticsEvent("android.crash", null, timestamp)
        val events = listOf(event)
        try {
            val analyticsRequest = serializeEvents(authorization, events, metadata)
            httpClient.post(
                FPTI_ANALYTICS_URL,
                analyticsRequest.toString(),
                null,
                authorization,
                HttpNoResponse()
            )
        } catch (e: JSONException) { /* ignored */
        }
    }

    @Throws(JSONException::class)
    private fun serializeEvents(
        authorization: Authorization?,
        events: List<AnalyticsEvent>,
        metadata: DeviceMetadata
    ): JSONObject {
        val batchParamsJSON = metadata.toJSON()
        authorization?.let {
            if (it is ClientToken) {
                batchParamsJSON.put(AUTHORIZATION_FINGERPRINT_KEY, it.bearer)
            } else {
                batchParamsJSON.put(TOKENIZATION_KEY, it.bearer)
            }
        }

        val eventsContainerJSON = JSONObject()
        eventsContainerJSON.put(BATCH_PARAMS_KEY, batchParamsJSON)

        val eventParamsJSON = JSONArray()
        for (analyticsEvent in events) {
            val singleEventJSON = JSONObject()
                .put(EVENT_NAME_KEY, analyticsEvent.name)

                .putOpt(PAYPAL_CONTEXT_ID_KEY, analyticsEvent.payPalContextId)
                .put(TIMESTAMP_KEY, analyticsEvent.timestamp)
                .put(TENANT_NAME_KEY, "Braintree")
            eventParamsJSON.put(singleEventJSON)
        }
        eventsContainerJSON.put(EVENT_PARAMS_KEY, eventParamsJSON)

        // Single-element "events" array required by FPTI formatting
        val eventsArray = JSONArray(arrayOf(eventsContainerJSON))
        return JSONObject().put(EVENTS_CONTAINER_KEY, eventsArray)
    }

    companion object {
        private const val FPTI_ANALYTICS_URL = "https://api-m.paypal.com/v1/tracking/batch/events"
        private const val PAYPAL_CONTEXT_ID_KEY = "paypal_context_id"
        private const val TOKENIZATION_KEY = "tokenization_key"

        private const val AUTHORIZATION_FINGERPRINT_KEY = "authorization_fingerprint"

        private const val INVALID_TIMESTAMP: Long = -1
        private const val EVENTS_CONTAINER_KEY = "events"
        private const val BATCH_PARAMS_KEY = "batch_params"
        private const val EVENT_PARAMS_KEY = "event_params"
        private const val EVENT_NAME_KEY = "event_name"
        private const val TIMESTAMP_KEY = "t"
        private const val TENANT_NAME_KEY = "tenant_name"
        const val WORK_NAME_ANALYTICS_UPLOAD = "uploadAnalytics"
        const val WORK_NAME_ANALYTICS_WRITE = "writeAnalyticsToDb"
        const val WORK_INPUT_KEY_AUTHORIZATION = "authorization"
        const val WORK_INPUT_KEY_CONFIGURATION = "configuration"
        const val WORK_INPUT_KEY_EVENT_NAME = "eventName"
        const val WORK_INPUT_KEY_INTEGRATION = "integration"
        const val WORK_INPUT_KEY_SESSION_ID = "sessionId"
        const val WORK_INPUT_KEY_TIMESTAMP = "timestamp"
        const val WORK_INPUT_KEY_PAYPAL_CONTEXT_ID = "payPalContextId"
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
