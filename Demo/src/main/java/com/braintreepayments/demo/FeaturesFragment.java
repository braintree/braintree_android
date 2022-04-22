package com.braintreepayments.demo;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FeaturesFragment extends Fragment implements FeaturesAdapter.ItemClickListener {

    static final String EXTRA_COLLECT_DEVICE_DATA = "collect_device_data";

    private ActionBarController actionBarController = new ActionBarController();

    public FeaturesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_features, container, false);

        Context context = requireContext();
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager);

        DividerItemDecoration dividerItemDecoration =
            new DividerItemDecoration(context, layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        recyclerView.setAdapter(new FeaturesAdapter(this));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        actionBarController.updateTitle(this);
    }

    @Override
    public void onFeatureSelected(DemoFeature feature) {
        switch (feature) {
            case CREDIT_OR_DEBIT_CARDS:
                launchCards();
                break;
            case PAYPAL:
                launchPayPal();
                break;
            case VENMO:
                launchVenmo();
                break;
            case GOOGLE_PAY:
                launchGooglePay();
                break;
            case SAMSUNG_PAY:
                launchSamsungPay();
                break;
            case VISA_CHECKOUT:
                launchVisaCheckout();
                break;
            case LOCAL_PAYMENT:
                launchLocalPayment();
                break;
            case PREFERRED_PAYMENT_METHODS:
                launchPreferredPaymentMethods();
                break;
        }
    }

    private void launchCards() {
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_COLLECT_DEVICE_DATA, Settings.shouldCollectDeviceData(getActivity()));

        FeaturesFragmentDirections.ActionFeaturesFragmentToCardFragment action =
            FeaturesFragmentDirections.actionFeaturesFragmentToCardFragment();
        action.setShouldCollectDeviceData(Settings.shouldCollectDeviceData(getActivity()));

        navigateWith(action);
    }

    private void launchPayPal() {
        FeaturesFragmentDirections.ActionFeaturesFragmentToPayPalFragment action =
                FeaturesFragmentDirections.actionFeaturesFragmentToPayPalFragment();
        action.setShouldCollectDeviceData(Settings.shouldCollectDeviceData(getActivity()));

        navigateWith(action);
    }

    private void launchVenmo() {
        NavDirections action = FeaturesFragmentDirections.actionFeaturesFragmentToVenmoFragment();
        navigateWith(action);
    }

    private void launchGooglePay() {
        NavDirections action =
                FeaturesFragmentDirections.actionFeaturesFragmentToGooglePayFragment();
        navigateWith(action);
    }

    private void launchSamsungPay() {
        NavDirections action =
                FeaturesFragmentDirections.actionFeaturesFragmentToSamsungPayFragment();
        navigateWith(action);
    }

    private void launchVisaCheckout() {
        NavDirections action = FeaturesFragmentDirections.actionFeaturesFragmentToVisaCheckoutFragment();
        navigateWith(action);
    }

    private void launchLocalPayment() {
        NavDirections action =
                FeaturesFragmentDirections.actionFeaturesFragmentToLocalPaymentFragment();
        navigateWith(action);
    }

    private void launchPreferredPaymentMethods() {
        NavDirections action =
                FeaturesFragmentDirections.actionFeaturesFragmentToPreferredPaymentMethodsFragment();
        navigateWith(action);
    }

    private void navigateWith(NavDirections action) {
        Fragment parentFragment = getParentFragment();
        if (parentFragment != null) {
            NavController navController = NavHostFragment.findNavController(parentFragment);
            navController.navigate(action);
        }
    }
}