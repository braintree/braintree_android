package com.paypal.android.sdk.onetouch.core.config;

import com.paypal.android.sdk.onetouch.core.network.EnvironmentManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OAuth2Recipe extends Recipe<OAuth2Recipe> {

    static final String DEVELOP = "develop";
    private final Collection<String> mScope = new HashSet<>();
    private final Map<String, ConfigEndpoint> mEndpoints = new HashMap<>();
    private boolean mIsValidForAllScopes;

    @Override
    public OAuth2Recipe getThis() {
        return this;
    }

    public void validForScope(String singleScopeValue) {
        mScope.add(singleScopeValue);
    }

    public void withEndpoint(String name, ConfigEndpoint endpoint) {
        mEndpoints.put(name, endpoint);
    }

    public boolean isValidForScopes(Set<String> scopes) {
        if (mIsValidForAllScopes) {
            return true;
        } else {
            return scopes.containsAll(scopes);
        }
    }

    public void validForAllScopes() {
        mIsValidForAllScopes = true;
    }

    /**
     * 1. Look for exact match of environment. (Could be mock, live, or a particular host:port.) 2.
     * If environment is anything other than mock or live, then look for develop. 3. Look for live.
     * (There should always be a live endpoint specified in any v3 browser-switch recipe.)
     *
     * @param environment
     * @return
     */
    public ConfigEndpoint getEndpoint(String environment) {
        ConfigEndpoint configEndpoint;
        if (mEndpoints.containsKey(environment)) {
            configEndpoint = mEndpoints.get(environment);
        } else if (mEndpoints.containsKey(DEVELOP)) {
            configEndpoint = mEndpoints.get(DEVELOP);
        } else {
            // default to live as fallback
            configEndpoint = mEndpoints.get(EnvironmentManager.LIVE);
        }

        return configEndpoint;
    }
}
