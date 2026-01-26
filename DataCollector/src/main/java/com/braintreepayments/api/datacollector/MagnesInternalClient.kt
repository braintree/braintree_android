package com.braintreepayments.api.datacollector

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.Configuration
import lib.android.paypal.com.magnessdk.Environment
import lib.android.paypal.com.magnessdk.InvalidInputException
import lib.android.paypal.com.magnessdk.MagnesResult
import lib.android.paypal.com.magnessdk.MagnesSDK
import lib.android.paypal.com.magnessdk.MagnesSettings
import lib.android.paypal.com.magnessdk.MagnesSource
import lib.android.paypal.com.magnessdk.MagnesSubmitStatus

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MagnesInternalClient(
    private val magnesSDK: MagnesSDK = MagnesSDK.getInstance()
) {

    @MainThread
    internal fun getClientMetadataId(
        context: Context?,
        configuration: Configuration?,
        request: DataCollectorInternalRequest?,
        callback: (String?, Exception?) -> Unit
    ) {
        if (context == null) {
            callback(null, IllegalArgumentException("Context is null"))
            return
        }
        if (configuration == null) {
            callback(null, IllegalArgumentException("Configuration is null"))
            return
        }
        if (request == null) {
            callback(null, IllegalArgumentException("Request is null"))
            return
        }

        val btEnvironment = configuration.environment
        val magnesEnvironment =
            if (btEnvironment.equals("sandbox", ignoreCase = true)) {
                Environment.SANDBOX
            } else {
                Environment.LIVE
            }

        try {
            val magnesSettingsBuilder: MagnesSettings.Builder =
                MagnesSettings.Builder(context.applicationContext)
                    .setMagnesSource(MagnesSource.BRAINTREE)
                    .disableBeacon(request.isDisableBeacon)
                    .setMagnesEnvironment(magnesEnvironment)
                    .setAppGuid(request.applicationGuid ?: "")
                    .setHasUserLocationConsent(request.hasUserLocationConsent)

            magnesSDK.setUp(magnesSettingsBuilder.build())
            lateinit var result: MagnesResult
            result = magnesSDK.collectAndSubmit(
                context.applicationContext,
                request.clientMetadataId,
                request.additionalData
            ) { status, _ ->
                // Callback is invoked when device data collection and submit API completes
                when (status) {
                    MagnesSubmitStatus.SUCCESS -> callback(result.paypalClientMetaDataId, null)
                    MagnesSubmitStatus.ERROR -> callback(null, CallbackSubmitException.SubmitError())
                    MagnesSubmitStatus.TIMEOUT -> callback(null, CallbackSubmitException.SubmitTimeout())
                    else -> callback(null, CallbackSubmitException.Unknown(status.toString()))
                }
            }
        } catch (e: InvalidInputException) {
            // Either clientMetadataId or appGuid exceeds their character limit
            Log.e(
                "Exception",
                "Error fetching client metadata ID. Contact Braintree Support for assistance.",
                e
            )
            callback(null, e)
        }
    }
}
