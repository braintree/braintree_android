package com.paypal.android.networking.processing;

import android.util.Log;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Logs all headers
 */
public class HeaderLoggingInterceptor implements Interceptor {
    private static final String TAG = HeaderLoggingInterceptor.class.getSimpleName();

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        for (Map.Entry<String, List<String>> header : originalRequest.headers().toMultimap().entrySet()) {
            for(String headerValue: header.getValue()) {
                Log.d(TAG, header.getKey() + "=" + headerValue);
            }
        }

        // does nothing
        return chain.proceed(originalRequest);
    }
}
