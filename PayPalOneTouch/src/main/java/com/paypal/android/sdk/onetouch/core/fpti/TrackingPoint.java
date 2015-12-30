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

    private final String mC;
    private final String mD;
    private final boolean mHasError;

    TrackingPoint(String c, String d, boolean hasError) {
        mC = c;
        mD = d;
        mHasError = hasError;
    }

    TrackingPoint(String c, String d) {
        this(c, d, false);
    }

    public String getCd() {
        return mC + ":" + mD;
    }

    public boolean hasError() {
        return mHasError;
    }
}
