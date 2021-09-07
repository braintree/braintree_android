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
        String path = EnvironmentHelper.getGatewayIp();

        if (!TextUtils.isEmpty(BuildConfig.GATEWAY_PORT)) {
            path = path + ":" + BuildConfig.GATEWAY_PORT;
        }

        if (!path.startsWith("http")) {
            path = "http://" + path;
        }

        return path;
    }
}
