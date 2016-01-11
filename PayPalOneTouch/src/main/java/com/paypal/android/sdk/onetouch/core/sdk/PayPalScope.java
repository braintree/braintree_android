package com.paypal.android.sdk.onetouch.core.sdk;

import java.util.Collection;
import java.util.HashSet;

/**
 * Scopes that a user may be asked to consented to.  This class is for internal use only.
 */
public enum PayPalScope {

    /**
     * Allows payments to be made in the future using PayPal
     */
    FUTURE_PAYMENTS("https://uri.paypal.com/services/payments/futurepayments", false),

    /**
     * Account profile
     */
    PROFILE("profile", true),

    /**
     * PayPal attributes - personal and account information
     */
    PAYPAL_ATTRIBUTES("https://uri.paypal.com/services/paypalattributes", true),

    /**
     * openid
     */
    OPENID("openid", true),

    /**
     * email
     */
    EMAIL("email", true),

    /**
     * address
     */
    ADDRESS("address", true),

    /**
     * phone
     */
    PHONE("phone", true);

    public static final Collection<String> PROFILE_SHARING_SCOPE_URIS = new HashSet<String>() {{
        for (PayPalScope scope : PayPalScope.values()) {
            if (scope.mIsProfileSharingScope) {
                add(scope.getScopeUri());
            }
        }
    }};

    public static final Collection<String> SCOPE_URIS = new HashSet<String>() {{
        for (PayPalScope scope : PayPalScope.values()) {
            add(scope.getScopeUri());
        }
    }};

    private String mScopeUri;
    private boolean mIsProfileSharingScope;

    PayPalScope(String scopeUri, boolean isProfileSharingScope) {
        mScopeUri = scopeUri;
        mIsProfileSharingScope = isProfileSharingScope;
    }

    public String getScopeUri() {
        return mScopeUri;
    }
}
