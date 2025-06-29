package com.braintreepayments.api.core

import android.content.Context
import androidx.annotation.RestrictTo

/**
 * Component class that is created when the BT SDK is launched. It contains dependencies that need to be injected that
 * contain Context.
 */
internal class SdkComponent(
    applicationContext: Context,
) {
    val configurationCache: ConfigurationCache = ConfigurationCache.getInstance(applicationContext)
    val deviceInspector: DeviceInspector = DeviceInspector(applicationContext)

    companion object {
        private var instance: SdkComponent? = null

        /**
         * Creates and returns a new instance of [SdkComponent], or returns the existing instance.
         */
        fun create(applicationContext: Context): SdkComponent {
            return instance ?: SdkComponent(applicationContext).also { sdkComponent ->
                instance = sdkComponent
            }
        }

        /**
         * Returns the instance of [SdkComponent]
         */
        fun getInstance(): SdkComponent {
            return checkNotNull(instance)
        }
    }
}

internal class ConfigurationCacheProvider {
    val configurationCache: ConfigurationCache
        get() = SdkComponent.getInstance().configurationCache
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DeviceInspectorProvider {
    val deviceInspector: DeviceInspector
        get() = SdkComponent.getInstance().deviceInspector
}
