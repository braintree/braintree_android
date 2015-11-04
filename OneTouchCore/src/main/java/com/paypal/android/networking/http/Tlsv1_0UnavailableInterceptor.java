package com.paypal.android.networking.http;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * This interceptor wraps requests attempted on < API 16 devices when TLSv1 is unavailable, since the exception
 * is a IllegalArgumentException (wut?), rather than an IOException.
 */
public class Tlsv1_0UnavailableInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        try {
            return chain.proceed(chain.request());
        } catch (IllegalArgumentException e) {
            throw new Tlsv1_0UnavailableException(e);
        }
    }
}
