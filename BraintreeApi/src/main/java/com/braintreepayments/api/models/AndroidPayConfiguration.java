package com.braintreepayments.api.models;

import com.google.gson.annotations.SerializedName;

public class AndroidPayConfiguration {

    @SerializedName("enabled") private boolean mEnabled;
    @SerializedName("googleAuthorizationFingerprint") private String mGoogleAuthorizationFingerprint;
    @SerializedName("environment") private String mEnvironment;
    @SerializedName("displayName") private String mDisplayName;

    public boolean isEnabled() {
        return mEnabled;
    }

    public String getGoogleAuthorizationFingerprint() {
        return mGoogleAuthorizationFingerprint;
    }

    public String getEnvironment() {
        return mEnvironment;
    }

    public String getDisplayName() {
        return mDisplayName;
    }
}
