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
}
