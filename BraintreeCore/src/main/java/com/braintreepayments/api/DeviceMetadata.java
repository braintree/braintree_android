package com.braintreepayments.api;

class DeviceMetadata {

    private String appVersion;
    private String dropInVersion;
    private String networkType;
    private String userOrientation;

    private boolean isPayPalInstalled;
    private boolean isVenmoInstalled;

    DeviceMetadata(
        String appVersion,
        String dropInVersion,
        String networkType,
        String userOrientation,
        boolean isPayPalInstalled,
        boolean isVenmoInstalled
    ) {
        this.appVersion = appVersion;
        this.dropInVersion = dropInVersion;
        this.networkType = networkType;
        this.userOrientation = userOrientation;
        this.isPayPalInstalled = isPayPalInstalled;
        this.isVenmoInstalled = isVenmoInstalled;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getDropInVersion() {
        return dropInVersion;
    }

    public String getNetworkType() {
        return networkType;
    }

    public String getUserOrientation() {
        return userOrientation;
    }

    public boolean isPayPalInstalled() {
        return isPayPalInstalled;
    }

    public boolean isVenmoInstalled() {
        return isVenmoInstalled;
    }
}
