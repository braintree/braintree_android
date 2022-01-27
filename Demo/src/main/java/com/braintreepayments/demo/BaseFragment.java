package com.braintreepayments.demo;

import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.PaymentMethodNonce;

public abstract class BaseFragment extends Fragment {

    @CallSuper
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        getActivity().setProgressBarIndeterminateVisibility(true);
    }

    @CallSuper
    public void onCancel(int requestCode) {
        getActivity().setProgressBarIndeterminateVisibility(false);
        Log.d(getClass().getSimpleName(), "Cancel received: " + requestCode);
    }

    @CallSuper
    protected void handleError(Exception error) {
        getActivity().setProgressBarIndeterminateVisibility(false);

        Log.d(getClass().getSimpleName(), "Error received (" + error.getClass() + "): " + error.getMessage());
        Log.d(getClass().getSimpleName(), error.toString());

        showDialog("An error occurred (" + error.getClass() + "): " + error.getMessage());
    }

    protected DemoActivity getDemoActivity() {
        FragmentActivity activity = getActivity();
        if (activity instanceof DemoActivity) {
            return ((DemoActivity) activity);
        }
        return null;
    }

    protected void showDialog(String message) {
        DemoActivity demoActivity = getDemoActivity();
        if (demoActivity != null) {
            demoActivity.showDialog(message);
        }
    }

    protected BraintreeClient getBraintreeClient() {
        DemoActivity demoActivity = getDemoActivity();
        if (demoActivity != null) {
            return demoActivity.getBraintreeClient();
        }
        return null;
    }
}
