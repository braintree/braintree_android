package com.braintreepayments.demo;

public enum TabFragment {
    DEMO(0, "Demo"),
    CONFIG(1, "Config"),
    SETTINGS(2, "Settings");

    public static TabFragment from(int value) {
        for (TabFragment tabFragment : TabFragment.values()) {
            if (tabFragment.value == value) {
                return tabFragment;
            }
        }
        return null;
    }

    private final int value;
    private final String displayName;

    TabFragment(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
