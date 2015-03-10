package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.braintreepayments.api.models.CoinbaseConfiguration;
import com.braintreepayments.api.models.Configuration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/* package */ class Coinbase {

    private static final String UTF_8 = "UTF-8";

    private Context mContext;
    private Configuration mConfiguration;

    /* package */ Coinbase(Context context, Configuration configuration) {
        mContext = context;
        mConfiguration = configuration;
    }

    /* package */ boolean isAvailable() {
        return mConfiguration.isCoinbaseEnabled();
    }

    /* package */ Intent getLaunchIntent() {
        CoinbaseConfiguration configuration = mConfiguration.getCoinbase();

        try {
            String url = "https://www.coinbase.com/oauth/authorize?response_type=code" +
                    "&client_id=" + URLEncoder.encode(configuration.getClientId(), UTF_8) +
                    "&scope=" + URLEncoder.encode(configuration.getScopes(), UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(configuration.getRedirectUrl(), UTF_8) +
                    "&meta%5Bauthorizations_merchant_account%5D=" +
                    URLEncoder.encode(configuration.getMerchantAccount(), UTF_8);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            return intent;
        } catch (UnsupportedEncodingException e) {
            if (BuildConfig.DEBUG) {
                throw new RuntimeException(e.getMessage());
            }
            return null;
        }
    }
}
