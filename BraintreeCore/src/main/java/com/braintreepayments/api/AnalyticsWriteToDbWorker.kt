package com.braintreepayments.api

import android.content.Context
import androidx.work.WorkerParameters

/**
 * Class for background analytics tasks.
 * This class is used internally by the SDK and should not be used directly.
 * It is not subject to semantic versioning and may change at any time.
 * @suppress
 */
open class AnalyticsWriteToDbWorker(
    context: Context,
    params: WorkerParameters
) : AnalyticsBaseWorker(context, params) {

    override fun doWork(): Result {
        val analyticsClient = createAnalyticsClientFromInputData()
        return analyticsClient.writeAnalytics(inputData)
    }
}
