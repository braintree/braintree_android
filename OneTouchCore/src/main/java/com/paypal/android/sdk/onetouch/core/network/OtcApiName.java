package com.paypal.android.sdk.onetouch.core.network;

import com.paypal.android.networking.http.HttpMethod;
import com.paypal.android.networking.request.ApiInfo;

import java.util.ArrayList;
import java.util.List;

public class OtcApiName implements ApiInfo {
    private final ApiName mApiName;

    public OtcApiName(ApiName apiName) {
        this.mApiName = apiName;
    }

    @Override
    public String getName() {
        return mApiName.name();
    }

    @Override
    public HttpMethod getMethod() {
        return mApiName.getMethod();
    }

    @Override
    public String getUrl() {
        return mApiName.getUrl();
    }

    public boolean isOverrideBaseUrl() {
        return mApiName.isOverrideBaseUrl();
    }

    public static List<OtcApiName> getAllValues() {
        List<OtcApiName> values = new ArrayList<>();
        for (ApiName apiName : ApiName.values()) {
            values.add(new OtcApiName(apiName));
        }
        return values;
    }

    /**
     * These are the server APIs that are implemented by the library.
     */
    public enum ApiName {
        /**
         * FPTI @see com.paypal.android.sdk.payments.base.jsonapi.FptiRequest
         */
        FptiRequest(HttpMethod.POST, null /*never read*/),

        /**
         * Config file location
         */
        ConfigFileRequest(HttpMethod.GET,
                "https://www.paypalobjects.com/webstatic/otc/otc-config.android.json", true);

        private HttpMethod mHttpMethod;
        private String mUrl;
        private boolean mOverrideBaseUrl;

        ApiName(HttpMethod mHttpMethod, String url, boolean overrideBaseUrl) {
            this.mHttpMethod = mHttpMethod;
            this.mUrl = url;
            this.mOverrideBaseUrl = overrideBaseUrl;
        }

        ApiName(HttpMethod mHttpMethod, String url) {
            this(mHttpMethod, url, false);
        }

        HttpMethod getMethod() {
            return this.mHttpMethod;
        }

        String getUrl() {
            return mUrl;
        }

        boolean isOverrideBaseUrl() {
            return mOverrideBaseUrl;
        }
    }
}
