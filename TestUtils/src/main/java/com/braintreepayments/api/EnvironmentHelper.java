package com.braintreepayments.api;

import android.text.TextUtils;

import com.braintree.api.testutils.BuildConfig;

public class EnvironmentHelper {

    public static String getLocalhostIp() {
        return BuildConfig.LOCALHOST_IP;
    }

    public static String getGatewayIp() {
        return getLocalhostIp();
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
