package com.braintreepayments.demo;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;

import androidx.fragment.app.Fragment;

public class ChangeAuthAndEnvironmentFragment extends Fragment {

    private AutoCompleteTextView environmentTextView;
    private AutoCompleteTextView authorizationTextView;

    public ChangeAuthAndEnvironmentFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int viewId = R.layout.fragment_change_auth_and_environment;
        View view = inflater.inflate(viewId, container, false);

        environmentTextView = view.findViewById(R.id.environment_text_view);
        authorizationTextView = view.findViewById(R.id.authorization_text_view);

        Context context = requireContext();
        environmentTextView.setText(Settings.getEnvironment(context));
        authorizationTextView.setText(Settings.getAuthorizationType(context));

        return view;
    }
}