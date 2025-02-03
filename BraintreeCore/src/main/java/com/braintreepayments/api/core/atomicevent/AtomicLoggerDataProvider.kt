package com.braintreepayments.api.core.atomicevent

import androidx.annotation.Keep

@Keep
open class AtomicLoggerDataProvider(
    open val metricType: AtomicLoggerMetricEventType,
    open val domain: AtomicLoggerDomain? = null,
    open val interaction: String,
    open val status: AtomicLoggerStatus? = null,
    open val interactionType: String? = null,
    open val navType: String? = null,
    open val task: String? = null,
    open val flow: String? = null,
    open val startDomain: AtomicLoggerDomain? = null,
    open val wasResumed: String? = null,
    open val isCrossApp: String?  = null,
    open val viewName: String? = null,
    open val startViewName: String? = null,
    open val startTask: String? = null,
    open val startPath: String? = null,
    open val path: String? = null,
    open val platform: String? = null,
    open val atomicLibVersion: String? = "0.16.0",
    open val metricValue: Long? = null,
    open val guid: String? = null
) {
    open fun getPayload(): AnalyticsPayload {
        return AnalyticsPayload(
            type = "metric",
            value = AnalyticsPayloadValue(
                dimensions = Dimensions(
                    domain = domain?.domain,
                    startDomain = startDomain?.domain,
                    wasResumed = wasResumed,
                    isCrossApp = isCrossApp,
                    interaction = interaction,
                    status = status?.statusType,
                    interactionType = interactionType,
                    navType,
                    task,
                    flow,
                    viewName,
                    startViewName,
                    startTask,
                    startPath,
                    path,
                    atomicLibVersion,
                    platform,
                    guid = guid
                ),
                metricEventName = metricType.metricEventName,
                metricId = metricType.metricId,
                metricType= metricType.metricType,
                metricValue = metricValue
            )
        )
    }
}