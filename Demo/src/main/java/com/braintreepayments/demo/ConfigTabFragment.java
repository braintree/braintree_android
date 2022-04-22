package com.braintreepayments.demo;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.fragment.app.Fragment;

public class ConfigTabFragment extends Fragment {

    private AutoCompleteTextView environmentTextView;
    private AutoCompleteTextView authorizationTextView;

    private ActionBarController actionBarController = new ActionBarController();

    public ConfigTabFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int viewId = R.layout.fragment_configuration;
        View view = inflater.inflate(viewId, container, false);

        Context context = requireContext();

        environmentTextView = view.findViewById(R.id.environment_text_view);
        environmentTextView.setText(Settings.getEnvironment(context));
        environmentTextView.setAdapter(createEnvironmentsAdapter());

        authorizationTextView = view.findViewById(R.id.authorization_text_view);
        authorizationTextView.setText(Settings.getAuthorizationType(context));
        authorizationTextView.setAdapter(createAuthorizationTypesAdapter());

        environmentTextView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                updateSettingsAndActionBarTitle();
            }
        });

        authorizationTextView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                updateSettingsAndActionBarTitle();
            }
        });
        return view;
    }

    private void updateSettingsAndActionBarTitle() {
        Context context = requireContext();

        String environment = environmentTextView.getText().toString();
        Settings.setEnvironment(context, environment);

        String authType = authorizationTextView.getText().toString();
        Settings.setAuthorizationType(context, authType);

        actionBarController.updateTitle(this);
    }

    private ArrayAdapter<String> createEnvironmentsAdapter() {
        String[] environments = getResources().getStringArray(R.array.environments);
        return new ArrayAdapter(requireContext(), R.layout.dropdown_item, environments);
    }

    private ArrayAdapter<String> createAuthorizationTypesAdapter() {
        String[] authorizationTypes = getResources().getStringArray(R.array.authorization_types);
        return new ArrayAdapter(requireContext(), R.layout.dropdown_item, authorizationTypes);
    }
}