package com.braintreepayments.api.shadows;

import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import java.util.HashMap;
import java.util.Map;

public class BraintreeCookieManager extends CookieManager {

    private Map<String, Boolean> acceptThirdPartyCookieMap = new HashMap<>();

    @Override
    public void setAcceptCookie(boolean accept) {}

    @Override
    public boolean acceptCookie() {
        return false;
    }

    @Override
    public void setAcceptThirdPartyCookies(WebView webview, boolean accept) {
        acceptThirdPartyCookieMap.put(webview.getClass().getName(), accept);
    }

    @Override
    public boolean acceptThirdPartyCookies(WebView webview) {
        return acceptThirdPartyCookieMap.containsKey(webview.getClass().getName()) && acceptThirdPartyCookieMap
                .get(webview.getClass().getName());
    }

    @Override
    public void setCookie(String url, String value) {}

    @Override
    public void setCookie(String url, String value, ValueCallback<Boolean> callback) {}

    @Override
    public String getCookie(String url) {
        return null;
    }

    @Override
    public void removeSessionCookie() {}

    @Override
    public void removeSessionCookies(ValueCallback<Boolean> callback) {}

    @Override
    public void removeAllCookie() {}

    @Override
    public void removeAllCookies(ValueCallback<Boolean> callback) {}

    @Override
    public boolean hasCookies() {
        return false;
    }

    @Override
    public void removeExpiredCookie() {}

    @Override
    public void flush() {}
}
