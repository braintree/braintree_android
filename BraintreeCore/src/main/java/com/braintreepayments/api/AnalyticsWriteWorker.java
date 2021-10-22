package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

public class AnalyticsWriteWorker extends AnalyticsBaseWorker {

    public AnalyticsWriteWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AnalyticsClient analyticsClient = createAnalyticsClientFromInputData();
        return analyticsClient.writeAnalytics(getApplicationContext(), getInputData());
    }
}
