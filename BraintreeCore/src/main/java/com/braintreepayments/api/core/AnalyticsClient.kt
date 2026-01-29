package com.braintreepayments.api.core

import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.Time
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("SwallowedException", "TooGenericExceptionCaught")
class AnalyticsClient internal constructor(
    private val analyticsApi: AnalyticsApi = AnalyticsApi(),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val analyticsEventRepository: AnalyticsEventRepository = AnalyticsEventRepository.instance,
    private val time: Time = Time(),
    private val configurationLoader: ConfigurationLoader = ConfigurationLoader.instance,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher),
) {

    fun sendEvent(
        eventName: String,
        analyticsEventParams: AnalyticsEventParams = AnalyticsEventParams(),
        sendImmediately: Boolean = true,
    ) {
        val event = AnalyticsEvent(
            name = eventName,
            timestamp = time.currentTime,
            contextId = analyticsEventParams.contextId,
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
            fundingSource = analyticsParamRepository.fundingSource,
            didEnablePayPalAppSwitch = analyticsParamRepository.didEnablePayPalAppSwitch,
            didPayPalServerAttemptAppSwitch = analyticsParamRepository.didPayPalServerAttemptAppSwitch,
            didSdkAttemptAppSwitch = analyticsParamRepository.didSdkAttemptAppSwitch,
        )
        if (sendImmediately) {
            coroutineScope.launch {
                val configResult = configurationLoader.loadConfiguration()
                if (configResult is ConfigurationLoaderResult.Success) {
                    executeEventsApi(event, configResult.configuration)
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
