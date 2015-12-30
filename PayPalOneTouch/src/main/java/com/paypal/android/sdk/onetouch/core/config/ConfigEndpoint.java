package com.paypal.android.sdk.onetouch.core.config;

public class ConfigEndpoint {

    private String mName;
    private String mUrl;
    private String mCertificate;

    public ConfigEndpoint name(String name) {
        mName = name;
        return this;
    }

    public ConfigEndpoint url(String url) {
        mUrl = url;
        return this;
    }

    public ConfigEndpoint certificate(String certificate) {
        mCertificate = certificate;
        return this;
    }

    public String getName() {
        return mName;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getCertificate() {
        return mCertificate;
    }
}
