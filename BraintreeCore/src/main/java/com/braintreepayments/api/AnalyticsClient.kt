package com.braintreepayments.api

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.braintreepayments.api.AnalyticsDatabase.Companion.getInstance
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
    private var lastKnownAnalyticsUrl: String? = null

    constructor(context: Context) : this(
        BraintreeHttpClient(),
        getInstance(context.applicationContext),
        WorkManager.getInstance(context.applicationContext),
        DeviceInspector()
    )

    fun sendEvent(
        configuration: Configuration,
        eventName: String?,
        sessionId: String?,
        integration: String?,
        authorization: Authorization
    ) {
        val timestamp = System.currentTimeMillis()
        sendEvent(configuration, eventName, sessionId, integration, timestamp, authorization)
    }

    @VisibleForTesting
    fun sendEvent(
        configuration: Configuration,
        eventName: String?,
        sessionId: String?,
        integration: String?,
        timestamp: Long,
        authorization: Authorization
    ): UUID {
        lastKnownAnalyticsUrl = configuration.analyticsUrl
        scheduleAnalyticsWrite("android.$eventName", timestamp, authorization)
        return scheduleAnalyticsUpload(configuration, authorization, sessionId, integration)
    }

    private fun scheduleAnalyticsWrite(
        eventName: String, timestamp: Long, authorization: Authorization
    ) {
        val inputData = Data.Builder()
            .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
            .putString(WORK_INPUT_KEY_EVENT_NAME, eventName)
            .putLong(WORK_INPUT_KEY_TIMESTAMP, timestamp)
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
        val timestamp = inputData.getLong(WORK_INPUT_KEY_TIMESTAMP, INVALID_TIMESTAMP)

        return if (eventName == null || timestamp == INVALID_TIMESTAMP) {
            ListenableWorker.Result.failure()
        } else {
            val event = AnalyticsEvent(eventName, timestamp)
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
                    val metadata = deviceInspector.getDeviceMetadata(context, sessionId, integration)
                    val analyticsRequest = serializeEvents(authorization, events, metadata)
                    configuration?.analyticsUrl?.let { analyticsUrl ->
                        httpClient.post(
                            analyticsUrl, analyticsRequest.toString(), configuration, authorization
                        )
                        analyticsEventDao.deleteEvents(events)
                    }
                }
                ListenableWorker.Result.success()
            } catch (e: Exception) {
                ListenableWorker.Result.failure()
            }
        }
    }

    fun reportCrash(
        context: Context?, sessionId: String?, integration: String?, authorization: Authorization?
    ) {
        reportCrash(context, sessionId, integration, System.currentTimeMillis(), authorization)
    }

    @VisibleForTesting
    fun reportCrash(
        context: Context?,
        sessionId: String?,
        integration: String?,
        timestamp: Long,
        authorization: Authorization?
    ) {
        if (authorization == null) {
            return
        }
        val metadata = deviceInspector.getDeviceMetadata(context, sessionId, integration)
        val event = AnalyticsEvent("android.crash", timestamp)
        val events = listOf(event)
        try {
            val analyticsRequest = serializeEvents(authorization, events, metadata)
            lastKnownAnalyticsUrl?.let { analyticsUrl ->
                httpClient.post(
                    analyticsUrl,
                    analyticsRequest.toString(),
                    null,
                    authorization,
                    HttpNoResponse()
                )
            }
        } catch (e: JSONException) { /* ignored */
        }
    }

    @Throws(JSONException::class)
    private fun serializeEvents(
        authorization: Authorization?, events: List<AnalyticsEvent>, metadata: DeviceMetadata
    ): JSONObject {
        val requestObject = JSONObject()
        authorization?.let {
            if (it is ClientToken) {
                requestObject.put(AUTHORIZATION_FINGERPRINT_KEY, it.bearer)
            } else {
                requestObject.put(TOKENIZATION_KEY, it.bearer)
            }
        }

        requestObject.put(META_KEY, metadata.toJSON())
        val eventObjects = JSONArray()
        var eventObject: JSONObject
        for (analyticsEvent in events) {
            eventObject = JSONObject()
                .put(KIND_KEY, analyticsEvent.name)
                .put(TIMESTAMP_KEY, analyticsEvent.timestamp)
            eventObjects.put(eventObject)
        }
        requestObject.put(ANALYTICS_KEY, eventObjects)
        return requestObject
    }

    companion object {
        private const val ANALYTICS_KEY = "analytics"
        private const val KIND_KEY = "kind"
        private const val TIMESTAMP_KEY = "timestamp"
        private const val META_KEY = "_meta"
        private const val TOKENIZATION_KEY = "tokenization_key"
        private const val AUTHORIZATION_FINGERPRINT_KEY = "authorization_fingerprint"
        private const val INVALID_TIMESTAMP: Long = -1
        const val WORK_NAME_ANALYTICS_UPLOAD = "uploadAnalytics"
        const val WORK_NAME_ANALYTICS_WRITE = "writeAnalyticsToDb"
        const val WORK_INPUT_KEY_AUTHORIZATION = "authorization"
        const val WORK_INPUT_KEY_CONFIGURATION = "configuration"
        const val WORK_INPUT_KEY_EVENT_NAME = "eventName"
        const val WORK_INPUT_KEY_INTEGRATION = "integration"
        const val WORK_INPUT_KEY_SESSION_ID = "sessionId"
        const val WORK_INPUT_KEY_TIMESTAMP = "timestamp"
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
