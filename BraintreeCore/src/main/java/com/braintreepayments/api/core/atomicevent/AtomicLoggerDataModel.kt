package com.braintreepayments.api.core.atomicevent

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
enum class AtomicLoggerMetricEventType(
    val metricEventName: String,
    val metricId: String,
    val metricType: String
) {
    START("ui_wait_start", "pp.xo.ui.ci.count", "counter"),
    END("ui_wait_end", "pp.xo.ui.ci.count", "counter"),
    EXPONENTIAL_TIME("user_wait_time_exponential", "pp.xo.ui.ci.count", "histogram")
}


@Keep
enum class AtomicLoggerDomain(val domain: String) {
    XO("xo"),
    InAppCheckout("inappcheckout"),
    BT_SDK("bt-sdk")
}

@Keep
enum class AtomicLoggerStatus(val statusType: String) {
    OK("Ok"),
    ERROR("Error"),
    FCI("Fci"),
    CANCELLED("Cancelled")
}


// Data Classes for Payloads
@Keep
data class AnalyticsPayload(
    @SerializedName("type") val type: String?,
    @SerializedName("value") val value: AnalyticsPayloadValue?
)

@Keep
data class AnalyticsPayloadValue(
    @SerializedName("dimensions") val dimensions: Dimensions?,
    @SerializedName("metricEventName") val metricEventName: String?,
    @SerializedName("metricId") val metricId: String?,
    @SerializedName("metricType") val metricType: String,
    @SerializedName("metricValue") val metricValue: Long? = null
)

@Keep
data class Dimensions(
    @SerializedName("domain") val domain: String?,
    @SerializedName("start_domain") val startDomain: String?,
    @SerializedName("was_resumed") val wasResumed: String?,
    @SerializedName("is_cross_app") val isCrossApp: String?,
    @SerializedName("interaction") val interaction: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("interaction_type") val interactionType: String?,
    @SerializedName("nav_type") val navType: String?,
    @SerializedName("task") val task: String?,
    @SerializedName("flow") val flow: String?,
    @SerializedName("view_name") val viewName: String?,
    @SerializedName("start_view_name") val startViewName: String?,
    @SerializedName("start_task") val startTask: String?,
    @SerializedName("start_path") val startPath: String?,
    @SerializedName("path") val path: String?,
    @SerializedName("atomic_lib_version") val atomicLibVersion: String?,
    @SerializedName("component") val component: String? = "android_app",
    @SerializedName("guid") val guid: String?
)