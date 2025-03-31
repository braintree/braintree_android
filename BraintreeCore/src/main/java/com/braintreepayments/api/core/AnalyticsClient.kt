package com.braintreepayments.api.core

import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.Time
import org.json.JSONException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("SwallowedException", "TooGenericExceptionCaught")
class AnalyticsClient internal constructor(
    private val analyticsApi: AnalyticsApi = AnalyticsApi(),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val analyticsEventRepository: AnalyticsEventRepository = AnalyticsEventRepository.instance,
    private val time: Time = Time(),
    private val configurationLoader: ConfigurationLoader = ConfigurationLoader.instance,
) {

    fun sendEvent(
        eventName: String,
        analyticsEventParams: AnalyticsEventParams = AnalyticsEventParams(),
        sendImmediately: Boolean = true,
    ) {
        val event = AnalyticsEvent(
            name = eventName,
            timestamp = time.currentTime,
            payPalContextId = analyticsEventParams.payPalContextId,
            linkType = analyticsParamRepository.linkType?.stringValue,
            isVaultRequest = analyticsEventParams.isVaultRequest,
            startTime = analyticsEventParams.startTime,
            endTime = analyticsEventParams.endTime,
            endpoint = analyticsEventParams.endpoint,
            experiment = analyticsEventParams.experiment,
            appSwitchUrl = analyticsEventParams.appSwitchUrl,
            shopperSessionId = analyticsEventParams.shopperSessionId,
            buttonType = analyticsEventParams.buttonType,
            buttonOrder = analyticsEventParams.buttonOrder,
            pageType = analyticsEventParams.pageType,
            errorDescription = analyticsEventParams.errorDescription,
            merchantEnabledAppSwitch = analyticsParamRepository.merchantEnabledAppSwitch,
            payPalServerSideAttemptedAppSwitch = analyticsParamRepository.payPalServerSideAttemptedAppSwitch
        )
        if (sendImmediately) {
            configurationLoader.loadConfiguration { result ->
                if (result is ConfigurationLoaderResult.Success) {
                    executeEventsApi(event, result.configuration)
                }
            }
        } else {
            analyticsEventRepository.addEvent(event)
        }
    }

    fun reportCrash(configuration: Configuration?) {
        val event = AnalyticsEvent(
            name = "crash",
            timestamp = time.currentTime
        )
        try {
            executeEventsApi(event, configuration)
        } catch (e: JSONException) { /* ignored */
        }
    }

    private fun executeEventsApi(
        event: AnalyticsEvent,
        configuration: Configuration?,
    ) {
        val events = analyticsEventRepository.flushAndReturnEvents().toMutableList()
        events.add(event)
        analyticsApi.execute(events, configuration)
    }

    companion object {
        val lazyInstance: Lazy<AnalyticsClient> = lazy { AnalyticsClient() }
    }
}
