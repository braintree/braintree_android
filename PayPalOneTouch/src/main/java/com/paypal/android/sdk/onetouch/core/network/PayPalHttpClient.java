package com.paypal.android.sdk.onetouch.core.network;

import android.support.annotation.VisibleForTesting;

import com.braintreepayments.api.internal.HttpClient;
import com.braintreepayments.api.internal.TLSSocketFactory;
import com.paypal.android.sdk.onetouch.core.BuildConfig;
import com.paypal.android.sdk.onetouch.core.base.DeviceInspector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

public class PayPalHttpClient extends HttpClient<PayPalHttpClient> {

    public PayPalHttpClient() {
        setUserAgent(String.format("PayPalSDK/PayPalOneTouch-Android %s (%s; %s; %s)", BuildConfig.VERSION_NAME,
                DeviceInspector.getOs(), DeviceInspector.getDeviceName(), BuildConfig.DEBUG ? "debug;" : ""));
        setConnectTimeout((int) TimeUnit.SECONDS.toMillis(90));

        try {
            setSSLSocketFactory(new TLSSocketFactory(PayPalCertificate.getCertInputStream()));
        } catch (SSLException e) {
            setSSLSocketFactory(null);
        }
    }

    @VisibleForTesting
    @Override
    protected HttpURLConnection init(String url) throws IOException {
        return super.init(url);
    }
}
