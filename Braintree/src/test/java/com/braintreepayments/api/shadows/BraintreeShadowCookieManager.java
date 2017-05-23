package com.braintreepayments.api.shadows;

import android.webkit.CookieManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(CookieManager.class)
public class BraintreeShadowCookieManager {

    private static BraintreeCookieManager sBraintreeCookieManager = new BraintreeCookieManager();

    @Implementation
    public static CookieManager getInstance() {
        return sBraintreeCookieManager;
    }
}
