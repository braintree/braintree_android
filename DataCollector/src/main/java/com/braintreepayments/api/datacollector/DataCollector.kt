package com.braintreepayments.api.datacollector

import android.content.Context
import android.text.TextUtils
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.braintreepayments.api.BraintreeClient
import com.braintreepayments.api.Configuration
import com.braintreepayments.api.UUIDHelper
import org.json.JSONException
import org.json.JSONObject

/**
 * PayPalDataCollector is used to collect PayPal specific device information to aid in fraud detection and prevention.
 */
class DataCollector @VisibleForTesting internal constructor(
    private val braintreeClient: BraintreeClient,
    private val magnesInternalClient: MagnesInternalClient,
    private val uuidHelper: UUIDHelper
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

    internal constructor(braintreeClient: BraintreeClient) : this(
        braintreeClient,
        MagnesInternalClient(),
        UUIDHelper()
    )

    /**
     * @suppress
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getPayPalInstallationGUID(context: Context?): String {
        return uuidHelper.getInstallationGUID(context)
    }

    /**
     * Gets a Client Metadata ID at the time of payment activity. Once a user initiates a PayPal payment
     * from their device, PayPal uses the Client Metadata ID to verify that the payment is
     * originating from a valid, user-consented device and application. This helps reduce fraud and
     * decrease declines. This method MUST be called prior to initiating a pre-consented payment (a
     * "future payment") from a mobile device. Pass the result to your server, to include in the
     * payment request sent to PayPal. Do not otherwise cache or store this value.
     *
     * @param context       Android Context
     * @param configuration The merchant configuration
     */
    @MainThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getClientMetadataId(context: Context?, configuration: Configuration?): String {
        val request = DataCollectorRequest()
            .setApplicationGuid(getPayPalInstallationGUID(context))
        return getClientMetadataId(context, request, configuration)
    }

    /**
     * Gets a Client Metadata ID at the time of payment activity. Once a user initiates a PayPal payment
     * from their device, PayPal uses the Client Metadata ID to verify that the payment is
     * originating from a valid, user-consented device and application. This helps reduce fraud and
     * decrease declines. This method MUST be called prior to initiating a pre-consented payment (a
     * "future payment") from a mobile device. Pass the result to your server, to include in the
     * payment request sent to PayPal. Do not otherwise cache or store this value.
     *
     * @param context       Android Context.
     * @param request       configures what data to collect.
     * @param configuration the merchant configuration
     *
     * @suppress
     */
    @MainThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    fun getClientMetadataId(
        context: Context?,
        request: DataCollectorRequest?,
        configuration: Configuration?
    ): String {
        return magnesInternalClient.getClientMetadataId(context, configuration, request)
    }

    /**
     * Collects device data based on your merchant configuration.
     *
     *
     * We recommend that you call this method as early as possible, e.g. at app launch. If that's too early,
     * call it at the beginning of customer checkout.
     *
     *
     * Use the return value on your server, e.g. with `Transaction.sale`.
     *
     * @param context  Android Context
     * @param callback [DataCollectorCallback]
     */
    fun collectDeviceData(context: Context, callback: DataCollectorCallback) {
        collectDeviceData(context, null, callback)
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
     * @param riskCorrelationId Optional client metadata id
     * @param callback          [DataCollectorCallback]
     */
    fun collectDeviceData(
        context: Context,
        riskCorrelationId: String?,
        callback: DataCollectorCallback
    ) {
        braintreeClient.getConfiguration { configuration: Configuration?, error: Exception? ->
            if (configuration != null) {
                val deviceData = JSONObject()
                try {
                    val request = DataCollectorRequest()
                        .setApplicationGuid(getPayPalInstallationGUID(context))
                    if (riskCorrelationId != null) {
                        request.setRiskCorrelationId(riskCorrelationId)
                    }
                    val correlationId =
                        magnesInternalClient.getClientMetadataId(context, configuration, request)
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