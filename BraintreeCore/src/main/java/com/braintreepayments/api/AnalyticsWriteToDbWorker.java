package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

/**
 * Class to write analytics events to the database.
 * This class is used internally by the SDK and should not be used directly.
 */
public class AnalyticsWriteToDbWorker extends AnalyticsBaseWorker {

    public AnalyticsWriteToDbWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AnalyticsClient analyticsClient = createAnalyticsClientFromInputData();
        return analyticsClient.writeAnalytics(getApplicationContext(), getInputData());
    }
}
