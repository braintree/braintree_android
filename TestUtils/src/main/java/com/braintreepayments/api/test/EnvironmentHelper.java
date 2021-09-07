package com.braintreepayments.api.test;

import android.text.TextUtils;

import com.braintreepayments.api.testutils.BuildConfig;

public class EnvironmentHelper {

    public static String getLocalhostIp() {
        return BuildConfig.LOCALHOST_IP;
    }

    public static String getGatewayIp() {
        return BuildConfig.GATEWAY_IP;
    }

    public static String getGatewayPath() {
        return "https://api.sandbox.braintreegateway.com:443";
    }
}
