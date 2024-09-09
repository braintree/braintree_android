package com.braintreepayments.api.core

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Class for background analytics tasks.
 * This class is used internally by the SDK and should not be used directly.
 * It is not subject to semantic versioning and may change at any time.
 * @suppress
 */
internal class AnalyticsUploadWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val analyticsClient = AnalyticsClient(applicationContext)
        return analyticsClient.performAnalyticsUpload(inputData)
    }
}
