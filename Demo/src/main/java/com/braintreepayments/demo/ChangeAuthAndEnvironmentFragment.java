package com.braintreepayments.demo;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class ChangeAuthAndEnvironmentFragment extends Fragment {

    private Button submitButton;

    private AutoCompleteTextView environmentTextView;
    private AutoCompleteTextView authorizationTextView;

    private ActionBarController actionBarController = new ActionBarController();

    public ChangeAuthAndEnvironmentFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int viewId = R.layout.fragment_change_auth_and_environment;
        View view = inflater.inflate(viewId, container, false);

        environmentTextView = view.findViewById(R.id.environment_text_view);
        authorizationTextView = view.findViewById(R.id.authorization_text_view);

        submitButton = view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(v -> updateSettingsAndReturnToMainFragment());

        Context context = requireContext();
        environmentTextView.setText(Settings.getEnvironment(context));
        environmentTextView.setAdapter(createEnvironmentsAdapter());

        authorizationTextView.setText(Settings.getAuthorizationType(context));
        authorizationTextView.setAdapter(createAuthorizationTypesAdapter());

        return view;
    }

    private void updateSettingsAndReturnToMainFragment() {
        Context context = requireContext();

        String environment = environmentTextView.getText().toString();
        Settings.setEnvironment(context, environment);

        String authType = authorizationTextView.getText().toString();
        Settings.setAuthorizationType(context, authType);

        actionBarController.updateTitle(this);

        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_goto_main);
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