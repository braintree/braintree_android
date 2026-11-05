package com.braintreepayments.api.core

import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.Time
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
) {

    @Deprecated("Kept for modules compiled against an earlier release. Pass the Authorization of the calling client.")
    fun sendEvent(
        eventName: String,
        analyticsEventParams: AnalyticsEventParams = AnalyticsEventParams(),
        sendImmediately: Boolean = true,
    ) {
        // Callers compiled before authorization was passed per event only: attributes the event to the newest client.
        val authorization = merchantRepository.authorizationOrNull ?: return
        sendEvent(eventName, authorization, analyticsEventParams, sendImmediately)
    }

    fun sendEvent(
        eventName: String,
        authorization: Authorization,
        analyticsEventParams: AnalyticsEventParams = AnalyticsEventParams(),
        sendImmediately: Boolean = true,
    ) {
        val event = AnalyticsEvent(
            name = eventName,
            timestamp = time.currentTime,
            contextId = analyticsEventParams.contextId,
            linkType = analyticsParamRepository.linkType?.stringValue,
            isVaultRequest = analyticsEventParams.isVaultRequest,
            shouldRequestBillingAgreement = analyticsParamRepository.shouldRequestBillingAgreement,
            recurringBillingPlanType = analyticsParamRepository.recurringBillingPlanType,
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
            uiType = analyticsEventParams.uiType,
            fundingSource = analyticsParamRepository.fundingSource,
            didEnablePayPalAppSwitch = analyticsParamRepository.didEnablePayPalAppSwitch,
            didPayPalServerAttemptAppSwitch = analyticsParamRepository.didPayPalServerAttemptAppSwitch,
            didSdkAttemptAppSwitch = analyticsParamRepository.didSdkAttemptAppSwitch,
        )
        if (sendImmediately) {
            coroutineScope.launch {
                send(event, authorization)
            }
        } else {
            analyticsEventRepository.addEvent(event, authorization)
        }
    }

    fun reportCrash(authorization: Authorization) {
        val event = AnalyticsEvent(
            name = "crash",
            timestamp = time.currentTime
        )
        coroutineScope.launch {
            send(event, authorization)
        }
    }

    private suspend fun send(trigger: AnalyticsEvent, authorization: Authorization) {
        val configuration = loadConfiguration(authorization) ?: return
        val batches = analyticsEventRepository.flushAndReturnEvents()
        val ownEvents = batches.filter { it.authorization == authorization }.flatMap { it.events }
        execute(ownEvents + trigger, configuration, authorization)
        for (batch in batches) {
            if (batch.authorization == authorization) continue
            coroutineScope.launch {
                // A failed configuration load drops the batch; retrying an expired authorization
                // would refetch configuration on every send.
                loadConfiguration(batch.authorization)?.let {
                    execute(batch.events, it, batch.authorization)
                }
            }
        }
    }

    private suspend fun loadConfiguration(authorization: Authorization): Configuration? {
        return try {
            (configurationLoader.loadConfiguration(authorization) as? ConfigurationLoaderResult.Success)
                ?.configuration
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun execute(
        events: List<AnalyticsEvent>,
        configuration: Configuration?,
        authorization: Authorization,
    ) {
        try {
            analyticsApi.execute(events, configuration, authorization)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) { /* a failed send must not cancel the shared analytics scope */ }
    }

    companion object {
        val lazyInstance: Lazy<AnalyticsClient> = lazy { AnalyticsClient() }
    }
}
