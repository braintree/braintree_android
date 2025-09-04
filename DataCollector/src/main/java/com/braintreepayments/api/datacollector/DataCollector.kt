package com.braintreepayments.api.datacollector

import android.content.Context
import android.text.TextUtils
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.UUIDHelper
import org.json.JSONException
import org.json.JSONObject

/**
 * PayPalDataCollector is used to collect PayPal specific device information to aid in fraud detection and prevention.
 */
class DataCollector @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    private val braintreeClient: BraintreeClient,
    private val magnesInternalClient: MagnesInternalClient = MagnesInternalClient(),
    private val uuidHelper: UUIDHelper = UUIDHelper()
) {
    /**
     * Initializes a new [DataCollector] instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    constructor(context: Context, authorization: String) : this(
        BraintreeClient(
            context,
            authorization
        )
    )

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getPayPalInstallationGUID(context: Context): String {
        return uuidHelper.getInstallationGUID(context)
    }

    /**
     * @suppress
     */
    @MainThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getClientMetadataId(
        context: Context,
        configuration: Configuration?,
        hasUserLocationConsent: Boolean
    ): String {
        val request = DataCollectorInternalRequest(hasUserLocationConsent).apply {
            applicationGuid = getPayPalInstallationGUID(context)
        }
        return getClientMetadataId(context, request, configuration)
    }

    /**
     * @suppress
     */
    @MainThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getClientMetadataId(
        context: Context?,
        request: DataCollectorInternalRequest?,
        configuration: Configuration?
    ): String {
        return magnesInternalClient.getClientMetadataId(context, configuration, request)
    }

    /**
     * Collects device data for PayPal APIs.
     *
     *
     * We recommend that you call this method as early as possible, e.g. at app launch. If that's too early,
     * call it at the beginning of customer checkout.
     *
     *
     * Use the return value on your server, e.g. with `Transaction.sale`.
     *
     * @param context           Android Context
     * @param request Optional client metadata id
     * @param callback          [DataCollectorCallback]
     */
    fun collectDeviceData(
        context: Context,
        request: DataCollectorRequest,
        callback: DataCollectorCallback
    ) {
        braintreeClient.getConfiguration { configuration: Configuration?, error: Exception? ->
            if (configuration != null) {
                val deviceData = JSONObject()
                try {
                    val internalRequest =
                        DataCollectorInternalRequest(request.hasUserLocationConsent).apply {
                            applicationGuid = getPayPalInstallationGUID(context)
                        }
                    if (request.riskCorrelationId != null) {
                        internalRequest.clientMetadataId = request.riskCorrelationId
                    }
                    val correlationId =
                        magnesInternalClient.getClientMetadataId(
                            context,
                            configuration,
                            internalRequest
                        )
                    if (!TextUtils.isEmpty(correlationId)) {
                        deviceData.put(CORRELATION_ID_KEY, correlationId)
                    }
                } catch (ignored: JSONException) {
                }
                callback.onDataCollectorResult(DataCollectorResult.Success(deviceData.toString()))
            } else if (error != null) {
                callback.onDataCollectorResult(DataCollectorResult.Failure(error))
            }
        }
    }

    companion object {
        private const val CORRELATION_ID_KEY = "correlation_id"
    }
}
