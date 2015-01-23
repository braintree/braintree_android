package com.braintreepayments.demo.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.braintreepayments.demo.R;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}
