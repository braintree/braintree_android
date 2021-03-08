package com.braintreepayments.api;

public class VenmoRequest {

    private boolean shouldVault;
    private String profileID;

    public VenmoRequest shouldVault(boolean shouldVault) {
        this.shouldVault = shouldVault;
        return this;
    }

    public VenmoRequest profileID(String profileID) {
        this.profileID = profileID;
        return this;
    }

    public boolean shouldVault() {
        return shouldVault;
    }

    public String getProfileID() {
        return profileID;
    }
}

