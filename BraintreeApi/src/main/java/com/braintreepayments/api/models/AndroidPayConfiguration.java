package com.braintreepayments.api.models;

public class AndroidPayConfiguration {

    private boolean enabled;
    private String googleAuthorizationFingerprint;
    private String environment;
    private String displayName;

    public boolean isEnabled() {
        return enabled;
    }

    public String getGoogleAuthorizationFingerprint() {
        return googleAuthorizationFingerprint;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getDisplayName() {
        return displayName;
    }
}
