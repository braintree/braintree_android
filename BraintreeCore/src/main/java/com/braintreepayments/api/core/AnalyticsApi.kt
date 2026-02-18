package com.braintreepayments.api.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * This API call sends analytic events to FPTI.
 */
internal class AnalyticsApi(
    private val httpClient: BraintreeHttpClient = BraintreeHttpClient(),
    private val deviceInspector: DeviceInspector = DeviceInspectorProvider().deviceInspector,
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher)
) {

    fun execute(
        events: List<AnalyticsEvent>,
        configuration: Configuration?
    ) {
        val jsonEvents = events.map { mapAnalyticsEventToFPTIEventJSON(it) }
        val metadata = deviceInspector.getDeviceMetadata(
            context = merchantRepository.applicationContext,
            configuration = configuration,
            sessionId = analyticsParamRepository.sessionId,
            integration = merchantRepository.integrationType
        )
        val analyticsRequest =
            createFPTIPayload(merchantRepository.authorization, jsonEvents, metadata)
        coroutineScope.launch {
            httpClient.post(
                path = FPTI_ANALYTICS_URL,
                data = analyticsRequest.toString(),
                configuration = null,
                authorization = merchantRepository.authorization,
            )
        }
    }

    @Throws(JSONException::class)
    private fun createFPTIPayload(
        authorization: Authorization?,
        events: List<JSONObject>,
        metadata: DeviceMetadata
    ): JSONObject {
        val batchParamsJSON = mapDeviceMetadataToFPTIBatchParamsJSON(metadata)
        batchParamsJSON.put(FPTI_BATCH_KEY_SPACE_KEY, "SKDUYK")
        batchParamsJSON.put(FPTI_BATCH_KEY_PRODUCT_NAME, "BT_DCC")

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
            .putOpt(FPTI_KEY_CONTEXT_ID, event.contextId)
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
            .putOpt(FPTI_KEY_MERCHANT_ENABLED_APP_SWITCH, event.didEnablePayPalAppSwitch)
            .putOpt(FPTI_KEY_PAYPAL_RECEIVED_APP_SWITCH_URL, event.didPayPalServerAttemptAppSwitch)
            .putOpt(FPTI_KEY_ERROR_DESC, event.errorDescription)
            .putOpt(FPTI_KEY_CONTEXT_TYPE, if (event.isVaultRequest) "BA-TOKEN" else "EC-TOKEN")
            .putOpt(FPTI_KEY_PAYPAL_ATTEMPTED_APP_SWITCH, event.didSdkAttemptAppSwitch)
            .putOpt(FPTI_KEY_FUNDING_SOURCE, event.fundingSource)
            .putOpt(FPTI_KEY_IS_PURCHASE, event.isPurchaseFlow)
            .putOpt(FPTI_KEY_IS_BILLING_AGREEMENT, event.shouldRequestBillingAgreement)
            .putOpt(FPTI_KEY_BILLING_PLAN_TYPE, event.recurringBillingPlanType)
    }

    @Throws(JSONException::class)
    private fun mapDeviceMetadataToFPTIBatchParamsJSON(metadata: DeviceMetadata): JSONObject {
        val isVenmoInstalled =
            deviceInspector.isVenmoInstalled(merchantRepository.applicationContext)
        val isPayPalInstalled = deviceInspector.isPayPalInstalled()
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
        private const val FPTI_ANALYTICS_URL = "https://api-m.paypal.com/v1/tracking/batch/events"

        private const val FPTI_KEY_CONTEXT_ID = "context_id"
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
        private const val FPTI_KEY_MERCHANT_ENABLED_APP_SWITCH = "merchant_enabled_app_switch"
        private const val FPTI_KEY_PAYPAL_RECEIVED_APP_SWITCH_URL = "paypal_app_switch_url_received"
        private const val FPTI_KEY_PAYPAL_ATTEMPTED_APP_SWITCH = "attempted_app_switch"
        private const val FPTI_KEY_ERROR_DESC = "error_desc"
        private const val FPTI_KEY_CONTEXT_TYPE = "context_type"
        private const val FPTI_KEY_FUNDING_SOURCE = "funding_source"
        private const val FPTI_KEY_IS_PURCHASE = "is_purchase"
        private const val FPTI_KEY_IS_BILLING_AGREEMENT = "is_billing_agreement"
        private const val FPTI_KEY_BILLING_PLAN_TYPE = "billing_plan_type"
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
        private const val FPTI_BATCH_KEY_SPACE_KEY = "space_key"
        private const val FPTI_BATCH_KEY_PRODUCT_NAME = "product_name"
    }
}
