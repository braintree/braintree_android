package com.braintreepayments.api

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

// NEXT_MAJOR_VERSION: remove this class, it may no longer be needed

/**
 * Class for background analytics tasks.
 * This class is used internally by the SDK and should not be used directly.
 * It is not subject to semantic versioning and may change at any time.
 * @suppress
 */
abstract class AnalyticsBaseWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    internal open fun createAnalyticsClientFromInputData(): AnalyticsClient {
        return AnalyticsClient(applicationContext)
    }
}
