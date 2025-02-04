package com.braintreepayments.api.core.atomicevent

import android.util.Log
import com.braintreepayments.api.core.BraintreeHttpClient
import org.json.JSONArray
import com.google.gson.Gson
import com.braintreepayments.api.core.UUIDHelper

class AtomicEventManager internal constructor(
    private val httpClient: BraintreeHttpClient = BraintreeHttpClient()
) {

    companion object {
        private const val AE_URL = "https://www.msmaster.qa.paypal.com/xoplatform/logger/api/ae"

        fun create(): AtomicEventManager {
            return AtomicEventManager()
        }
    }

    // Start Atomic Event upload
    fun performStartAtomicEventsUpload() {

        val startAtomicLoggerDataProvider = AtomicLoggerDataProvider(
            metricType = AtomicLoggerMetricEventType.START,
            domain = AtomicLoggerDomain.BT_SDK,
            interaction = AtomicEventConstants.InteractionType.PAY_WITH_PAYPAL,
            status = AtomicLoggerStatus.OK,
            interactionType = AtomicEventConstants.InteractionType.CLICK,
            navType = AtomicEventConstants.NavType.NAVIGATE,
            task = AtomicEventConstants.Task.SELECT_VAULTED_CHECKOUT,
            flow = AtomicEventConstants.Flow.MODXO_VAULTED_NOT_RECURRING,
            startDomain = AtomicLoggerDomain.BT_SDK,
            wasResumed = "false",
            isCrossApp = "false",
            path = AtomicEventConstants.PATH.PAY,
            platform = AtomicEventConstants.PLATFORM,
            guid = UUIDHelper().formattedUUID
        )

        val gson = Gson()
        val atomicPayload = gson.toJson(JSONArray().put(startAtomicLoggerDataProvider.getPayload()))

        try {
            httpClient.post(
                path = AE_URL,
                data = atomicPayload,
                configuration = null,
                authorization = null,
                callback = null
            )
            Log.d("Trace-BT", "Atomic Server Logger API call invoked successfully.")
        } catch (e: Exception) {
            Log.d("Trace-BT", "Exception in performAtomicEventsUpload: $e")
        }
    }

    // End Atomic Event upload
    fun performEndAtomicEventUpload() {

        val endAtomicLoggerDataProvider = AtomicLoggerDataProvider(
            metricType = AtomicLoggerMetricEventType.END,
            domain = AtomicLoggerDomain.BT_SDK,
            startDomain = AtomicLoggerDomain.XO,
            wasResumed = "false",
            isCrossApp = "true",
            interaction = AtomicEventConstants.InteractionType.APPROVE_BILLING_AGREEMENT,
            status = AtomicLoggerStatus.OK,
            interactionType = AtomicEventConstants.InteractionType.RENDER,
            navType = AtomicEventConstants.NavType.NAVIGATE,
            task = AtomicEventConstants.Task.SELECT_AGREE_AND_CONTINUE,
            flow = AtomicEventConstants.Flow.MODXO_VAULTED_NOT_RECURRING,
            viewName = AtomicEventConstants.VIEW.RETURN_TO_MERCHANT,
            startViewName = AtomicEventConstants.VIEW.PAY_WITH_MODULE,
            startTask = AtomicEventConstants.Task.SELECT_AGREE_AND_CONTINUE,
            platform = AtomicEventConstants.PLATFORM,
            guid = UUIDHelper().formattedUUID
        )

        val gson = Gson()
        val atomicPayload = gson.toJson(JSONArray().put(endAtomicLoggerDataProvider.getPayload()))

        try {
            httpClient.post(
                path = AE_URL,
                data = atomicPayload,
                configuration = null,
                authorization = null,
                callback = null
            )
            Log.d("Trace-BT", "Atomic Server Logger API call invoked successfully.")
        } catch (e: Exception) {
            Log.d("Trace-BT", "Exception in performAtomicEventsUpload: $e")
        }
    }

    // Cancel Atomic Event upload
    fun performCancelEndAtomicEventUpload() {
        val endTimeAtomicLoggerDataProvider = AtomicLoggerDataProvider(
            metricType = AtomicLoggerMetricEventType.END,
            domain = AtomicLoggerDomain.BT_SDK,
            startDomain = AtomicLoggerDomain.XO,
            wasResumed = "false",
            isCrossApp = "true",
            interaction = AtomicEventConstants.InteractionType.CLICK_CANCEL_AND_RETURN_TO_MERCHANT,
            status = AtomicLoggerStatus.OK,
            navType = AtomicEventConstants.NavType.NAVIGATE,
            task = AtomicEventConstants.Task.SELECT_CANCEL_AND_RETURN_TO_MERCHANT_LINK,
            flow = AtomicEventConstants.Flow.MODXO_VAULTED_NOT_RECURRING,
            viewName = AtomicEventConstants.VIEW.RETURN_TO_MERCHANT,
            startViewName = AtomicEventConstants.VIEW.PAY_WITH_MODULE,
            startTask = AtomicEventConstants.Task.SELECT_CANCEL_AND_RETURN_TO_MERCHANT_LINK,
            platform = AtomicEventConstants.PLATFORM,
            guid = UUIDHelper().formattedUUID
        )

        val gson = Gson()
        val atomicPayload =
            gson.toJson(JSONArray().put(endTimeAtomicLoggerDataProvider.getPayload()))

        try {
            httpClient.post(
                path = AE_URL,
                data = atomicPayload,
                configuration = null,
                authorization = null,
                callback = null
            )
            Log.d("Trace-BT", "Atomic Server Logger API call invoked successfully.")
        } catch (e: Exception) {
            Log.d("Trace-BT", "Exception in performAtomicEventsUpload: $e")
        }
    }

    fun performEndEventUpload(endTime: Long?) {
        performEndAtomicEventUpload()
        performEndTimeExponentialAtomicEventUpload(endTime)
    }

    private fun performEndTimeExponentialAtomicEventUpload(endTime: Long?) {
        val endTimeAtomicLoggerDataProvider = AtomicLoggerDataProvider(
            metricType = AtomicLoggerMetricEventType.EXPONENTIAL_TIME,
            domain = AtomicLoggerDomain.BT_SDK,
            startDomain = AtomicLoggerDomain.XO,
            wasResumed = "false",
            isCrossApp = "true",
            interaction = AtomicEventConstants.InteractionType.APPROVE_BILLING_AGREEMENT,
            status = AtomicLoggerStatus.OK,
            navType = AtomicEventConstants.NavType.NAVIGATE,
            task = AtomicEventConstants.Task.SELECT_AGREE_AND_CONTINUE,
            flow = AtomicEventConstants.Flow.MODXO_VAULTED_NOT_RECURRING,
            viewName = AtomicEventConstants.VIEW.RETURN_TO_MERCHANT,
            startViewName = AtomicEventConstants.VIEW.PAY_WITH_MODULE,
            startTask = AtomicEventConstants.Task.SELECT_AGREE_AND_CONTINUE,
            platform = AtomicEventConstants.PLATFORM,
            metricValue = endTime,
            guid = UUIDHelper().formattedUUID
        )

        val gson = Gson()
        val atomicPayload =
            gson.toJson(JSONArray().put(endTimeAtomicLoggerDataProvider.getPayload()))

        try {
            httpClient.post(
                path = AE_URL,
                data = atomicPayload,
                configuration = null,
                authorization = null,
                callback = null
            )
            Log.d("Trace-BT", "Atomic Server Logger API call invoked successfully.")
        } catch (e: Exception) {
            Log.d("Trace-BT", "Exception in performAtomicEventsUpload: $e")
        }
    }

    fun performCancelEndEventUpload(endTime: Long?) {
        performCancelEndAtomicEventUpload()
        performCancelEndTimeExponentialAtomicEventUpload(endTime)
    }

    private fun performCancelEndTimeExponentialAtomicEventUpload(endTime: Long?) {
        val endTimeAtomicLoggerDataProvider = AtomicLoggerDataProvider(
            metricType = AtomicLoggerMetricEventType.EXPONENTIAL_TIME,
            domain = AtomicLoggerDomain.BT_SDK,
            startDomain = AtomicLoggerDomain.XO,
            wasResumed = "false",
            isCrossApp = "true",
            interaction = AtomicEventConstants.InteractionType.CLICK_CANCEL_AND_RETURN_TO_MERCHANT,
            status = AtomicLoggerStatus.OK,
            navType = AtomicEventConstants.NavType.NAVIGATE,
            task = AtomicEventConstants.Task.SELECT_CANCEL_AND_RETURN_TO_MERCHANT_LINK,
            flow = AtomicEventConstants.Flow.MODXO_VAULTED_NOT_RECURRING,
            viewName = AtomicEventConstants.VIEW.RETURN_TO_MERCHANT,
            startViewName = AtomicEventConstants.VIEW.PAY_WITH_MODULE,
            startTask = AtomicEventConstants.Task.SELECT_CANCEL_AND_RETURN_TO_MERCHANT_LINK,
            platform = AtomicEventConstants.PLATFORM,
            metricValue = endTime,
            guid = UUIDHelper().formattedUUID
        )

        val gson = Gson()
        val atomicPayload =
            gson.toJson(JSONArray().put(endTimeAtomicLoggerDataProvider.getPayload()))

        try {
            httpClient.post(
                path = AE_URL,
                data = atomicPayload,
                configuration = null,
                authorization = null,
                callback = null
            )
            Log.d("Trace-BT", "Atomic Server Logger API call invoked successfully.")
        } catch (e: Exception) {
            Log.d("Trace-BT", "Exception in performAtomicEventsUpload: $e")
        }
    }
}