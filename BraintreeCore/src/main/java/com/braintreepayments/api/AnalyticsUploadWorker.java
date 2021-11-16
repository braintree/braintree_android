package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;

import static com.braintreepayments.api.AnalyticsClient.ANALYTICS_INPUT_DATA_AUTHORIZATION_KEY;
import static com.braintreepayments.api.AnalyticsClient.ANALYTICS_INPUT_DATA_CONFIGURATION_KEY;

/**
 * Class to upload analytics events.
 * This class is used internally by the SDK and should not be used directly.
 */
public class AnalyticsUploadWorker extends Worker {

    public AnalyticsUploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        Authorization authorization = getAuthorizationFromData(inputData);
        Configuration configuration = getConfigurationFromData(inputData);
        if (authorization == null || configuration == null) {
            return Result.failure();
        }

        AnalyticsClient analyticsClient = new AnalyticsClient(authorization);
        try {
            analyticsClient.uploadAnalytics(getApplicationContext(), configuration);
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }

    private static Authorization getAuthorizationFromData(Data inputData) {
        if (inputData != null) {
            String authString = inputData.getString(ANALYTICS_INPUT_DATA_AUTHORIZATION_KEY);
            return Authorization.fromString(authString);
        }
        return null;
    }

    private static Configuration getConfigurationFromData(Data inputData) {
        if (inputData != null) {
            String configJson = inputData.getString(ANALYTICS_INPUT_DATA_CONFIGURATION_KEY);
            if (configJson != null) {
                try {
                    return Configuration.fromJson(configJson);
                } catch (JSONException e) { /* ignored */ }
            }
        }
        return null;
    }
}
