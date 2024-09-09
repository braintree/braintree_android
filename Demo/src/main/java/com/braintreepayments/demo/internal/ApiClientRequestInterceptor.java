package com.braintreepayments.demo.internal;

import androidx.annotation.NonNull;

import com.braintreepayments.demo.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ApiClientRequestInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Request modifiedRequest;

        modifiedRequest = request.newBuilder()
            .addHeader("User-Agent", "braintree/android-demo-app/" + BuildConfig.VERSION_NAME)
            .addHeader("Accept", "application/json")
            .build();
        return chain.proceed(modifiedRequest);
    }
}
