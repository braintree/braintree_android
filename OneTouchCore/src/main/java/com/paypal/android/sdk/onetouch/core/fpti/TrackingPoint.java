package com.paypal.android.sdk.onetouch.core.fpti;

public enum TrackingPoint {
    WalletIsPresent("checkwallet", "present"),
    WalletIsAbsent("checkwallet", "absent"),

    PreflightBrowser("preflight", "browser"),
    PreflightWallet("preflight", "wallet"),
    PreflightNone("preflight", "none"),

    SwitchToBrowser("switchaway", "browser"),
    SwitchToWallet("switchaway", "wallet"),

    Cancel("switchback", "cancel"),
    Return("switchback", "return"),
    Error("switchback", "cancel", true);


    private final String c;
    private final String d;
    private final boolean hasError;


    TrackingPoint(String c, String d, boolean hasError) {
        this.c = c;
        this.d = d;
        this.hasError = hasError;
    }

    TrackingPoint(String c, String d) {
        this(c, d, false);
    }

    public String getCd() {
        return c + ":" + d;
    }
    public boolean hasError(){
        return hasError;
    }
}
