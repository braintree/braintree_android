package com.braintreepayments.demo;

public enum TabFragment {
    FEATURES(0),
    CONFIG(1),
    SETTINGS(2);

    private final int value;

    TabFragment(int value) {
        this.value = value;
    }

    public static TabFragment from(int value) {
        for (TabFragment tabFragment : TabFragment.values()) {
            if (tabFragment.value == value) {
                return tabFragment;
            }
        }
        return null;
    }
}
