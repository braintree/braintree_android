package com.braintreepayments.api;

import static com.braintreepayments.api.AnalyticsClient.INPUT_DATA_EVENT_NAME;
import static com.braintreepayments.api.AnalyticsClient.INPUT_DATA_TIMESTAMP;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class AnalyticsWriteWorker extends Worker {

    public AnalyticsWriteWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        String eventName = inputData.getString(INPUT_DATA_EVENT_NAME);
        long timestamp = inputData.getLong(INPUT_DATA_TIMESTAMP, 0);

        AnalyticsEvent2 event = new AnalyticsEvent2(eventName, timestamp);
        AnalyticsDatabase2 db = AnalyticsDatabase2.getDatabase(getApplicationContext());

        AnalyticsEventDao analyticsEventDao = db.analyticsEventDao();
        analyticsEventDao.insertEvent(event);

        return Result.success();
    }
}
