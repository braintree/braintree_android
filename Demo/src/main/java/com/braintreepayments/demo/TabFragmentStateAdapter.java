package com.braintreepayments.demo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.braintreepayments.demo.fragments.SettingsTabFragment;

public class TabFragmentStateAdapter extends FragmentStateAdapter {

    public TabFragmentStateAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Tab tab = Tab.from(position);
        switch (tab) {
            case DEMO:
                return new DemoTabFragment();
            case CONFIG:
                return new ConfigTabFragment();
            case SETTINGS:
            default:
                return new SettingsTabFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
