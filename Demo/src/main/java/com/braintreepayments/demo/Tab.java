package com.braintreepayments.demo;

public enum Tab {
    DEMO(0, "Demo"),
    CONFIG(1, "Config"),
    SETTINGS(2, "Settings");

    public static Tab from(int value) {
        for (Tab tab : Tab.values()) {
            if (tab.value == value) {
                return tab;
            }
        }
        return null;
    }

    private final int value;
    private final String displayName;

    Tab(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
