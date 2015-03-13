package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.braintreepayments.api.exceptions.CoinbaseException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.CoinbaseConfiguration;
import com.braintreepayments.api.models.Configuration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/* package */ class Coinbase {

    private static final String REDIRECT_URI_HOST = "coinbase";
    private static final String REDIRECT_URI_SCHEME_SUFFIX = "braintree";
    private static final String UTF_8 = "UTF-8";

    private Context mContext;
    private Configuration mConfiguration;

    public Coinbase(Context context, Configuration configuration) {
        mContext = context;
        mConfiguration = configuration;
    }

    public boolean isAvailable() {
        return mConfiguration.isCoinbaseEnabled();
    }

    public Intent getLaunchIntent() throws UnsupportedEncodingException {
        CoinbaseConfiguration configuration = mConfiguration.getCoinbase();

        String url = "https://www.coinbase.com/oauth/authorize?response_type=code" +
                "&client_id=" + URLEncoder.encode(configuration.getClientId(), UTF_8) +
                "&scope=" + URLEncoder.encode(configuration.getScopes(), UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(getRedirectUri(), UTF_8) +
                "&meta%5Bauthorizations_merchant_account%5D=" +
                URLEncoder.encode(configuration.getMerchantAccount(), UTF_8);

        return new Intent(mContext, BraintreeBrowserSwitchActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(BraintreeBrowserSwitchActivity.EXTRA_REQUEST_URL, url);
    }

    private String getRedirectUri() {
        return new Uri.Builder()
                .scheme(mContext.getPackageName() + "." + REDIRECT_URI_SCHEME_SUFFIX)
                .authority(REDIRECT_URI_HOST)
                .build()
                .toString();
    }

    public boolean canParseResponse(Uri responseUri) {
        return responseUri != null &&
                responseUri.getScheme().equals(
                        mContext.getPackageName() + "." + REDIRECT_URI_SCHEME_SUFFIX) &&
                responseUri.getHost().equals(REDIRECT_URI_HOST);
    }

    public String parseResponse(Uri responseUri)
            throws CoinbaseException, ConfigurationException {
        if (canParseResponse(responseUri)) {
            String code = responseUri.getQueryParameter("code");

            if (!TextUtils.isEmpty(code)) {
                return code;
            } else {
                throw new CoinbaseException(responseUri.getQueryParameter("error_description"));
            }
        }

        throw new ConfigurationException("Intent did not contain a well-formed OAuth response from Coinbase");
    }
}
