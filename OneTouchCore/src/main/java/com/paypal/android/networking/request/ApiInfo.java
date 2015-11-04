package com.paypal.android.networking.request;

import com.paypal.android.networking.http.HttpMethod;

public interface ApiInfo {
    String getName();

    HttpMethod getMethod();

    String getUrl();
}
