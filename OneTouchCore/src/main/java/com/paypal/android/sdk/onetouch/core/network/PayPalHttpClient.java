package com.paypal.android.sdk.onetouch.core.network;

import android.support.annotation.VisibleForTesting;

import com.braintreepayments.api.internal.HttpClient;
import com.braintreepayments.api.internal.TLSSocketFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

public class PayPalHttpClient extends HttpClient<PayPalHttpClient> {

    public PayPalHttpClient() {
        setUserAgent(OtcEnvironment.getUserAgent());
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
