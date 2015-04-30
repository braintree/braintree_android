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

/**
 * Class to generate Coinbase request url and parse Coinbase response.
 */
public class Coinbase {

    private static final String REDIRECT_URI_HOST = "coinbase";
    private static final String REDIRECT_URI_SCHEME_SUFFIX = "braintree";
    private static final String UTF_8 = "UTF-8";

    private Context mContext;
    private Configuration mConfiguration;

    protected Coinbase(Context context, Configuration configuration) {
        mContext = context;
        mConfiguration = configuration;
    }

    protected boolean isAvailable() {
        return mConfiguration.isCoinbaseEnabled();
    }

    protected Intent getLaunchIntent() throws UnsupportedEncodingException {
        CoinbaseConfiguration configuration = mConfiguration.getCoinbase();

        String url = configuration.getBaseURLForEnvironment() +
                "oauth/authorize?response_type=code" +
                "&client_id=" + URLEncoder.encode(configuration.getClientId(), UTF_8) +
                "&scope=" + URLEncoder.encode(configuration.getScopes(), UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(getRedirectUri(), UTF_8) +
                "&meta%5Bauthorizations_merchant_account%5D=" +
                URLEncoder.encode(configuration.getMerchantAccount(), UTF_8);

        return new Intent(mContext, BraintreeBrowserSwitchActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(BraintreeBrowserSwitchActivity.EXTRA_REQUEST_URL, url);
    }

    public String getRedirectUri() {
        return new Uri.Builder()
                .scheme(mContext.getPackageName() + "." + REDIRECT_URI_SCHEME_SUFFIX)
                .authority(REDIRECT_URI_HOST)
                .build()
                .toString();
    }

    /**
     * Method to check if a given {@link android.content.Intent} was a Coinbase response or not.
     *
     * @param context
     * @param intent Intent to check
     * @return {@code true} if {@link android.content.Intent} was a Coinbase response, {@code false}
     *         otherwise.
     */
    public static boolean canParseResponse(Context context, Intent intent) {
        Uri redirectUri = intent.getParcelableExtra(BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL);
        return canParseResponse(context, redirectUri);
    }

    protected static boolean canParseResponse(Context context, Uri responseUri) {
        return responseUri != null &&
                responseUri.getScheme().equals(
                        context.getPackageName() + "." + REDIRECT_URI_SCHEME_SUFFIX) &&
                responseUri.getHost().equals(REDIRECT_URI_HOST);
    }

    protected String parseResponse(Uri responseUri)
            throws CoinbaseException, ConfigurationException {
        if (Coinbase.canParseResponse(mContext, responseUri)) {
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
