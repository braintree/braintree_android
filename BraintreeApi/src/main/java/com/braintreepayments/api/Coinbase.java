package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

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

    public boolean canHandleResponse(Uri responseUri) {
        return responseUri != null &&
                responseUri.getScheme().equals(
                        mContext.getPackageName() + "." + REDIRECT_URI_SCHEME_SUFFIX) &&
                responseUri.getHost().equals(REDIRECT_URI_HOST);
    }

    public Response handleResponse(Uri responseUri) {
        if (canHandleResponse(responseUri)) {
            return new Response(responseUri.getQueryParameter("code"),
                    responseUri.getQueryParameter("error_description"));
        }
        return null;
    }

    /* package */ static class Response implements Parcelable {

        private String mCode;
        private String mErrorMessage;

        private Response(String code, String errorMessage) {
            mCode = code;
            mErrorMessage = errorMessage;
        }

        private Response(Parcel in) {
            mCode = in.readString();
            mErrorMessage = in.readString();
        }

        public boolean isSuccess() {
            return !TextUtils.isEmpty(mCode);
        }

        public String getCode() {
            return mCode;
        }

        public String getErrorMessage() {
            return mErrorMessage;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mCode);
            dest.writeString(mErrorMessage);
        }

        public static final Creator<Response> CREATOR = new Creator<Response>() {
            public Response createFromParcel(Parcel source) {
                return new Response(source);
            }

            public Response[] newArray(int size) {
                return new Response[size];
            }
        };
    }
}
