package com.braintreepayments.demo;

import java.util.Arrays;

public enum DemoTab {
    FEATURES(0),
    ENVIRONMENT(1),
    SETTINGS(2);

    private final int value;

    DemoTab(int value) {
        this.value = value;
    }

    public static DemoTab from(int value) {
        for (DemoTab demoTab : DemoTab.values()) {
            if (demoTab.value == value) {
                return demoTab;
            }
        }
        return null;
    }
}
