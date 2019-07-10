package com.braintreepayments.demo;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.os.strictmode.Violation;
import android.util.Log;

import com.braintreepayments.demo.internal.ApiClient;
import com.braintreepayments.demo.internal.ApiClientRequestInterceptor;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit.RestAdapter;

public class DemoApplication extends Application implements UncaughtExceptionHandler {

    private static ApiClient sApiClient;
    private static ExecutorService sExecutor;

    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    @Override
    public void onCreate() {
        // TODO: This is causing the Cardinal SDK to crash, we need to get them to fix it.
        if (BuildConfig.DEBUG) {
            StrictMode.ThreadPolicy.Builder threadPolicy = new StrictMode.ThreadPolicy.Builder()
                    .detectCustomSlowCalls()
                    .detectNetwork()
                    .penaltyLog();
            StrictMode.VmPolicy.Builder vmPolicy = new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedRegistrationObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectFileUriExposure()
                    .penaltyLog();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                sExecutor = Executors.newSingleThreadExecutor();

                threadPolicy.penaltyListener(sExecutor, new StrictMode.OnThreadViolationListener() {
                    @Override
                    public void onThreadViolation(Violation v) {
                        handleViolation(v);

                    }
                });
                vmPolicy.penaltyListener(sExecutor, new StrictMode.OnVmViolationListener() {
                    @Override
                    public void onVmViolation(Violation v) {
                        handleViolation(v);
                    }
                });
            }

            StrictMode.setThreadPolicy(threadPolicy.build());
            StrictMode.setVmPolicy(vmPolicy.build());
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


    private void handleViolation(Violation v) {
        Throwable cause = v.getCause();

        if (classExistsInThrowable("com.cardinalcommerce.cardinalmobilesdk.Tasks.NewtworkTask.CentinelChallengeTask", "doInBackground", cause)) {
            Log.d("handleViolation", "Cardinal has been notified of this strict mode violation");
        } else if (classExistsInThrowable("com.cardinalcommerce.cardinalmobilesdk.Tasks.NewtworkTask.CentinelApiInitTask", "doInBackground", cause)) {
            Log.d("handleViolation", "Cardinal has been notified of this strict mode violation");
        } else {
            throw new RuntimeException(v.getCause());
        }
    }

    private boolean classExistsInThrowable(String clazz, String method, Throwable t) {
        for (StackTraceElement stackTrace : t.getStackTrace()) {
            String causeClass = stackTrace.getClassName();
            String causeMethod = stackTrace.getMethodName();

            if (clazz.equals(causeClass) && method.equals(causeMethod)) {
                return true;
            }
        }

        return false;
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
