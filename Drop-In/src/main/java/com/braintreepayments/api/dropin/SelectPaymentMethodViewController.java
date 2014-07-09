package com.braintreepayments.api.dropin;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.dropin.view.PaymentMethodView;
import com.braintreepayments.api.models.PaymentMethod;

/**
 * {@link com.braintreepayments.api.dropin.BraintreeViewController} for handling manipulation of existing payment methods.
 */
public class SelectPaymentMethodViewController extends BraintreeViewController
        implements View.OnClickListener {

    // @formatter:off
    private static final String EXTRA_SELECTED_PAYMENT_METHOD = "com.braintreepayments.api.dropin.EXTRA_SELECTED_PAYMENT_METHOD";
    // @formatter:on

    /**
     * When adding new views, make sure to update {@link #onSaveInstanceState} and the proper
     * tests.
     */
    private PaymentMethodView mPaymentMethodView;
    private TextView mChangeMethodView;
    private PaymentMethodListAdapter mPaymentMethodListAdapter;
    private Button mSubmitButton;

    public SelectPaymentMethodViewController(BraintreePaymentActivity activity,
            Bundle savedInstanceState, View root, Braintree braintree, Customization customization) {
        super(activity, root, braintree, customization);
        mPaymentMethodView = findView(R.id.selected_payment_method_view);
        mPaymentMethodView.setOnClickListener(this);

        mChangeMethodView = findView(R.id.change_payment_method_link);
        mChangeMethodView.setOnClickListener(this);

        mSubmitButton = findView(R.id.select_payment_method_button);
        mSubmitButton.setOnClickListener(this);
        mSubmitButton.setText(getSubmitButtonText());

        PaymentMethod activePaymentMethod;
        if (savedInstanceState.containsKey(EXTRA_SELECTED_PAYMENT_METHOD)) {
            activePaymentMethod = (PaymentMethod) savedInstanceState
                    .getSerializable(EXTRA_SELECTED_PAYMENT_METHOD);
        } else {
            activePaymentMethod = getActivity().getActivePaymentMethod();
        }
        setupPaymentMethod(activePaymentMethod);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_SELECTED_PAYMENT_METHOD,
                getActivity().getActivePaymentMethod());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mPaymentMethodView.getId()) {
            if (getBraintree().getCachedPaymentMethods().size() > 1) {
                showPaymentMethodListDialog();
            }
        } else if (v.getId() == mChangeMethodView.getId()) {
            if (getBraintree().getCachedPaymentMethods().size() == 1) {
                launchFormView();
            } else {
                showPaymentMethodListDialog();
            }
        } else if (v.getId() == mSubmitButton.getId()) {
            mSubmitButton.setEnabled(false);
            getActivity().finalizeSelection();
        }
    }

    private void setupPaymentMethod(PaymentMethod method) {
        mPaymentMethodView.setPaymentMethodDetails(method.getTypeLabel(), method.getDescription());

        TextView link = findView(R.id.change_payment_method_link);
        if(getBraintree().getCachedPaymentMethods().size() == 1) {
            link.setText(R.string.add_payment_method);
        } else {
            link.setText(R.string.change_payment_method);
        }
    }

    private void showPaymentMethodListDialog() {
        mPaymentMethodListAdapter = new PaymentMethodListAdapter(getActivity(), this, getBraintree().getCachedPaymentMethods());

        ContextThemeWrapper contextThemeWrapper;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            contextThemeWrapper = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        }
        else {
            contextThemeWrapper = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Light_NoTitleBar);
        }

        new AlertDialog.Builder(contextThemeWrapper)
            .setTitle(R.string.choose_payment_method)
            .setAdapter(mPaymentMethodListAdapter, mPaymentMethodListAdapter)
            .setPositiveButton(R.string.add_new_payment_method, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    launchFormView();
                }
            })
            .show();
    }

    private void launchFormView() {
        getActivity().initAddPaymentMethodView();
    }

    protected void onPaymentMethodSelected(PaymentMethod paymentMethod) {
        getActivity().setActivePaymentMethod(paymentMethod);
        mPaymentMethodView.setPaymentMethodDetails(paymentMethod.getTypeLabel(),
                paymentMethod.getDescription());
    }
}
