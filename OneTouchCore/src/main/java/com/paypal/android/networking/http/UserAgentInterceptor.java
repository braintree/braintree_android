package com.paypal.android.networking.http;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * This interceptor adds a custom User-Agent.
 */
public class UserAgentInterceptor implements Interceptor {

    private static final String USER_AGENT_HEADER = "User-Agent";

    private final String mUserAgent;

    public UserAgentInterceptor(String userAgent) {
        mUserAgent = userAgent;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request requestWithUserAgent = originalRequest.newBuilder()
                .removeHeader(USER_AGENT_HEADER)
                .addHeader(USER_AGENT_HEADER, mUserAgent)
                .build();
        return chain.proceed(requestWithUserAgent);
    }
}