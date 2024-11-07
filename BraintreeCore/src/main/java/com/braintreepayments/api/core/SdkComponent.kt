package com.braintreepayments.api.core

import android.content.Context
import androidx.work.WorkManager

/**
 * Component class that is created when the BT SDK is launched. It contains dependencies that need to be injected that
 * contain Context.
 */
internal class SdkComponent(
    applicationContext: Context,
) {
    val analyticsDatabase: AnalyticsDatabase = AnalyticsDatabase.getInstance(applicationContext)
    val workManager: WorkManager = WorkManager.getInstance(applicationContext)

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

internal class AnalyticsDatabaseProvider {
    val analyticsDatabase: AnalyticsDatabase
        get() = SdkComponent.getInstance().analyticsDatabase
}

internal class WorkManagerProvider {
    val workManager: WorkManager
        get() = SdkComponent.getInstance().workManager
}