package com.braintreepayments.demo;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.util.Log;

import com.braintreepayments.demo.internal.ApiClient;
import com.braintreepayments.demo.internal.ApiClientRequestInterceptor;

import java.lang.Thread.UncaughtExceptionHandler;

import retrofit.RestAdapter;

public class DemoApplication extends Application implements UncaughtExceptionHandler {

    private static ApiClient sApiClient;

    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectCustomSlowCalls()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedRegistrationObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectFileUriExposure()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        super.onCreate();

        DeveloperTools.setup(this);

        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Log.e("Exception", "Uncaught Exception", ex);
        mDefaultExceptionHandler.uncaughtException(thread, ex);
    }

    static ApiClient getApiClient(Context context) {
        if (sApiClient == null) {
            sApiClient = new RestAdapter.Builder()
                    .setEndpoint(Settings.getEnvironmentUrl(context))
                    .setRequestInterceptor(new ApiClientRequestInterceptor())
                    .build()
                    .create(ApiClient.class);
        }

        return sApiClient;
    }

    static void resetApiClient() {
        sApiClient = null;
    }
}
