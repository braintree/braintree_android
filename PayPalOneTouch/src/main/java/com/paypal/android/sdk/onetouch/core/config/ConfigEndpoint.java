package com.paypal.android.sdk.onetouch.core.config;

public class ConfigEndpoint {

    public final String name;
    public final String url;
    public final String certificate;

    public ConfigEndpoint(String name, String url, String certificate) {
        this.name = name;
        this.url = url;
        this.certificate = certificate;
    }
}
