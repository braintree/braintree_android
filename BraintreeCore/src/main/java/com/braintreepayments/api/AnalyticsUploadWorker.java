package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

/**
 * Class for background analytics tasks.
 * This class is used internally by the SDK and should not be used directly.
 * It is not subject to semantic versioning and may change at any time.
 */
public class AnalyticsUploadWorker extends AnalyticsBaseWorker {

    public AnalyticsUploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AnalyticsClient analyticsClient = createAnalyticsClientFromInputData();
        return analyticsClient.uploadAnalytics(getApplicationContext(), getInputData());
    }
}
