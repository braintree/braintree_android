package com.braintreepayments.demo;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public class DemoTabFragment extends Fragment {

    private ActionBarController actionBarController = new ActionBarController();

    public DemoTabFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_demo_tab, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        actionBarController.updateTitle(this);
    }
}