package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

/**
 * Class to upload analytics events.
 * This class is used internally by the SDK and should not be used directly.
 */
public class AnalyticsUploadFromDbWorker extends AnalyticsBaseWorker {

    public AnalyticsUploadFromDbWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AnalyticsClient analyticsClient = createAnalyticsClientFromInputData();
        return analyticsClient.uploadAnalytics(getApplicationContext(), getInputData());
    }
}
