package com.paypal.android.sdk.onetouch.core.config;

import com.paypal.android.sdk.onetouch.core.network.EnvironmentManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OAuth2Recipe extends Recipe<OAuth2Recipe> {

    static final String DEVELOP = "develop";
    private final Collection<String> scope = new HashSet<>();
    private final Map<String, ConfigEndpoint> endpoints = new HashMap<>();
    private boolean isValidForAllScopes;

    @Override
    public OAuth2Recipe getThis() {
        return this;
    }

    public void validForScope(String singleScopeValue) {
        this.scope.add(singleScopeValue);
    }

    public void withEndpoint(String name, ConfigEndpoint endpoint) {
        this.endpoints.put(name, endpoint);
    }

    public boolean isValidForScopes(Set<String> scopes) {
        if (isValidForAllScopes) {
            return true;
        } else {
            return scopes.containsAll(scopes);
        }
    }

    public void validForAllScopes() {
        this.isValidForAllScopes = true;
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
        if (endpoints.containsKey(environment)) {
            configEndpoint = endpoints.get(environment);
        } else if (endpoints.containsKey(DEVELOP)) {
            configEndpoint = endpoints.get(DEVELOP);
        } else {
            // default to live as fallback
            configEndpoint = endpoints.get(EnvironmentManager.LIVE);
        }

        return configEndpoint;
    }
}
