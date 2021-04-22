package com.braintreepayments.demo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.braintreepayments.demo.internal.ApiClient;
import com.braintreepayments.demo.internal.ApiClientRequestInterceptor;

import java.lang.Thread.UncaughtExceptionHandler;

import retrofit.RestAdapter;

public class DemoApplication extends Application implements UncaughtExceptionHandler {

    private static ApiClient apiClient;

    private Thread.UncaughtExceptionHandler defaultExceptionHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        DeveloperTools.setup(this);

        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Log.e("Exception", "Uncaught Exception", ex);
        defaultExceptionHandler.uncaughtException(thread, ex);
    }

    static ApiClient getApiClient(Context context) {
        if (apiClient == null) {
            apiClient = new RestAdapter.Builder()
                    .setEndpoint(Settings.getEnvironmentUrl(context))
                    .setRequestInterceptor(new ApiClientRequestInterceptor())
                    .build()
                    .create(ApiClient.class);
        }

        return apiClient;
    }

    static void resetApiClient() {
        apiClient = null;
    }
}
