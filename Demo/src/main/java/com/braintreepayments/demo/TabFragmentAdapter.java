package com.braintreepayments.demo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.braintreepayments.demo.fragments.SettingsFragment;

public class TabFragmentAdapter extends FragmentStateAdapter {

    public TabFragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Tab tab = Tab.from(position);
        switch (tab) {
            case FEATURES:
                return NavHostFragment.create(R.navigation.nav_graph);
            case CONFIG:
                return new ConfigurationFragment();
            case SETTINGS:
            default:
                return new SettingsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
