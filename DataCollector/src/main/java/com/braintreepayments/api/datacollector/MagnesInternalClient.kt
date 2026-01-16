package com.braintreepayments.api.datacollector

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.Configuration
import lib.android.paypal.com.magnessdk.Environment
import lib.android.paypal.com.magnessdk.InvalidInputException
import lib.android.paypal.com.magnessdk.MagnesSDK
import lib.android.paypal.com.magnessdk.MagnesSettings
import lib.android.paypal.com.magnessdk.MagnesSource

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MagnesInternalClient(
    private val magnesSDK: MagnesSDK = MagnesSDK.getInstance()
) {

    @MainThread
    internal fun getClientMetadataId(
        context: Context?,
        configuration: Configuration?,
        request: DataCollectorInternalRequest?
        //add callback here ?
        //submitApiCallback
    ): String {
        if (context == null || configuration == null || request == null) {
            return ""
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
            val result = magnesSDK.collectAndSubmit(
                context.applicationContext,
                request.clientMetadataId,
                request.additionalData
            ) /*result ->  {  return result.paypalClientMetaDataId }*/
            return result.paypalClientMetaDataId
        } catch (e: InvalidInputException) {
            // Either clientMetadataId or appGuid exceeds their character limit
            Log.e(
                "Exception",
                "Error fetching client metadata ID. Contact Braintree Support for assistance.",
                e
            )
            return ""
        }
    }
}
