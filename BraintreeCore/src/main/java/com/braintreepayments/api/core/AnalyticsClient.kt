package com.braintreepayments.api.core

import android.content.Context
import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.Time
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("SwallowedException", "TooGenericExceptionCaught")
class AnalyticsClient internal constructor(
    private val httpClient: BraintreeHttpClient = BraintreeHttpClient(),
    private val deviceInspector: DeviceInspector = DeviceInspector(),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val time: Time = Time(),
    private val configurationLoader: ConfigurationLoader = ConfigurationLoader.instance,
    private val merchantRepository: MerchantRepository = MerchantRepository.instance
) {

    private val applicationContext: Context
        get() = merchantRepository.applicationContext

    fun sendEvent(
        eventName: String,
        analyticsEventParams: AnalyticsEventParams = AnalyticsEventParams()
    ) {
        val analyticsEvent = AnalyticsEvent(
            name = eventName,
            timestamp = time.currentTime,
            payPalContextId = analyticsEventParams.payPalContextId,
            linkType = analyticsEventParams.linkType,
            isVaultRequest = analyticsEventParams.isVaultRequest,
            startTime = analyticsEventParams.startTime,
            endTime = analyticsEventParams.endTime,
            endpoint = analyticsEventParams.endpoint,
            experiment = analyticsEventParams.experiment,
            appSwitchUrl = analyticsEventParams.appSwitchUrl,
            shopperSessionId = analyticsEventParams.shopperSessionId,
            buttonType = analyticsEventParams.buttonType,
            buttonOrder = analyticsEventParams.buttonOrder,
            pageType = analyticsEventParams.pageType
        )
        configurationLoader.loadConfiguration { result ->
            if (result is ConfigurationLoaderResult.Success) {

                val metadata = deviceInspector.getDeviceMetadata(
                    applicationContext,
                    result.configuration,
                    analyticsParamRepository.sessionId,
                    merchantRepository.integrationType
                )
                val eventJson = mapAnalyticsEventToFPTIEventJSON(analyticsEvent)
                val analyticsRequest = createFPTIPayload(merchantRepository.authorization, listOf(eventJson), metadata)
                httpClient.post(
                    path = FPTI_ANALYTICS_URL,
                    data = analyticsRequest.toString(),
                    configuration = result.configuration,
                    authorization = merchantRepository.authorization,
                    callback = null
                )
            }
        }
    }

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
        try {
            val analyticsRequest = createFPTIPayload(authorization, listOf(eventJSON), metadata)
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
        events: List<JSONObject>,
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
        for (event in events) {
            eventParamsJSON.put(event)
        }
        eventsContainerJSON.put(FPTI_KEY_EVENT_PARAMS, eventParamsJSON)

        // Single-element "events" array required by FPTI formatting
        val eventsArray = JSONArray(arrayOf(eventsContainerJSON))
        return JSONObject().put(FPTI_KEY_EVENTS, eventsArray)
    }

    private fun mapAnalyticsEventToFPTIEventJSON(event: AnalyticsEvent): JSONObject {
        return JSONObject()
            .put(FPTI_KEY_EVENT_NAME, event.name)
            .put(FPTI_KEY_TIMESTAMP, event.timestamp)
            .put(FPTI_KEY_IS_VAULT, event.isVaultRequest)
            .put(FPTI_KEY_TENANT_NAME, "Braintree")
            .putOpt(FPTI_KEY_PAYPAL_CONTEXT_ID, event.payPalContextId)
            .putOpt(FPTI_KEY_LINK_TYPE, event.linkType)
            .putOpt(FPTI_KEY_START_TIME, event.startTime)
            .putOpt(FPTI_KEY_END_TIME, event.endTime)
            .putOpt(FPTI_KEY_ENDPOINT, event.endpoint)
            .putOpt(FPTI_KEY_MERCHANT_EXPERIMENT, event.experiment)
            .putOpt(FPTI_KEY_URL, event.appSwitchUrl)
            .putOpt(FPTI_KEY_SHOPPER_SESSION_ID, event.shopperSessionId)
            .putOpt(FPTI_KEY_BUTTON_TYPE, event.buttonType)
            .putOpt(FPTI_KEY_BUTTON_POSITION, event.buttonOrder)
            .putOpt(FPTI_KEY_PAGE_TYPE, event.pageType)
    }

    @Throws(JSONException::class)
    private fun mapDeviceMetadataToFPTIBatchParamsJSON(metadata: DeviceMetadata): JSONObject {
        val isVenmoInstalled = deviceInspector.isVenmoInstalled(applicationContext)
        val isPayPalInstalled = deviceInspector.isPayPalInstalled(applicationContext)
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
                .put(FPTI_BATCH_KEY_PAYPAL_INSTALLED, isPayPalInstalled)
        }
    }

    companion object {

        val lazyInstance: Lazy<AnalyticsClient> = lazy { AnalyticsClient() }

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
        private const val FPTI_KEY_MERCHANT_EXPERIMENT = "experiment"
        private const val FPTI_KEY_URL = "url"
        private const val FPTI_KEY_SHOPPER_SESSION_ID = "shopper_session_id"
        private const val FPTI_KEY_BUTTON_TYPE = "button_type"
        private const val FPTI_KEY_BUTTON_POSITION = "button_position"
        private const val FPTI_KEY_PAGE_TYPE = "page_type"

        private const val FPTI_BATCH_KEY_VENMO_INSTALLED = "venmo_installed"
        private const val FPTI_BATCH_KEY_PAYPAL_INSTALLED = "paypal_installed"
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
    }
}
