package com.braintreepayments.api;

import static com.braintreepayments.api.AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Class for background analytics tasks.
 * This class is used internally by the SDK and should not be used directly.
 * It is not subject to semantic versioning and may change at any time.
 */
// NEXT_MAJOR_VERSION: remove this class, it may no longer be needed
public abstract class AnalyticsBaseWorker extends Worker {

    public AnalyticsBaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    protected AnalyticsClient createAnalyticsClientFromInputData() {
        return new AnalyticsClient(getApplicationContext());
    }
}
