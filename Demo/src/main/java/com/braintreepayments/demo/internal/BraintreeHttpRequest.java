package com.braintreepayments.demo.internal;

import com.braintreepayments.demo.BuildConfig;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class BraintreeHttpRequest implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request().newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", "braintree/android-demo-app/" + BuildConfig.VERSION_NAME)
                .removeHeader("Accept")
                .addHeader("Accept", "application/json")
                .build();

        return chain.proceed(request);
    }
}
