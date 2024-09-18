package com.braintreepayments.demo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.braintreepayments.demo.internal.ApiClient;
import com.braintreepayments.demo.internal.ApiClientRequestInterceptor;

import java.lang.Thread.UncaughtExceptionHandler;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DemoApplication extends Application implements UncaughtExceptionHandler {

    private static ApiClient apiClient;

    private Thread.UncaughtExceptionHandler defaultExceptionHandler;

    @Override
    public void onCreate() {
        super.onCreate();

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
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new ApiClientRequestInterceptor())
                .build();

            apiClient = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(Settings.getEnvironmentUrl(context))
                .client(okHttpClient)
                .build()
                .create(ApiClient.class);
        }

        return apiClient;
    }

    static void resetApiClient() {
        apiClient = null;
    }
}
