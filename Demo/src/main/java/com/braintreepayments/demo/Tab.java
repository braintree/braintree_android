package com.braintreepayments.demo;

public enum Tab {
    FEATURES(0),
    CONFIG(1),
    SETTINGS(2);

    private final int value;

    Tab(int value) {
        this.value = value;
    }

    public static Tab from(int value) {
        for (Tab tab : Tab.values()) {
            if (tab.value == value) {
                return tab;
            }
        }
        return null;
    }
}
