package com.paypal.android.sdk.onetouch.core.config;

public class ConfigEndpoint {

    private String mName;
    private String mUrl;
    private String mCertificate;

    public ConfigEndpoint name(String name) {
        this.mName = name;
        return this;
    }

    public ConfigEndpoint url(String url) {
        this.mUrl = url;
        return this;
    }

    public ConfigEndpoint certificate(String certificate) {
        this.mCertificate = certificate;
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
