package com.braintreepayments.api;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.braintreepayments.api.dropin.adapters.PaymentMethodListAdapter;
import com.braintreepayments.api.dropin.adapters.PaymentMethodListAdapter.PaymentMethodSelectedListener;
import com.braintreepayments.api.dropin.view.PaymentMethodView;
import com.braintreepayments.api.models.PaymentMethod;

/**
 * {@link BraintreeViewController} for handling manipulation of existing payment methods.
 */
public class SelectPaymentMethodViewController extends BraintreeViewController
        implements View.OnClickListener, PaymentMethodSelectedListener {

    // @formatter:off
    private static final String EXTRA_SELECTED_PAYMENT_METHOD = "com.braintreepayments.api.dropin.EXTRA_SELECTED_PAYMENT_METHOD";
    // @formatter:on

    /**
     * When adding new views, make sure to update {@link #onSaveInstanceState} and the proper
     * tests.
     */
    private PaymentMethodView mPaymentMethodView;
    private TextView mChangeMethodView;
    private Button mSubmitButton;

    private int mActivePaymentMethod;

    public SelectPaymentMethodViewController(BraintreePaymentActivity activity,
            Bundle savedInstanceState, View root, BraintreeFragment braintreeFragment,
            PaymentRequest paymentRequest) {
        super(activity, root, braintreeFragment, paymentRequest);
        mPaymentMethodView = findView(com.braintreepayments.api.dropin.R.id.bt_selected_payment_method_view);
        mPaymentMethodView.setOnClickListener(this);

        mChangeMethodView = findView(com.braintreepayments.api.dropin.R.id.bt_change_payment_method_link);
        mChangeMethodView.setOnClickListener(this);

        mSubmitButton = findView(com.braintreepayments.api.dropin.R.id.bt_select_payment_method_submit_button);
        mSubmitButton.setOnClickListener(this);
        mSubmitButton.setText(getSubmitButtonText());

        if (savedInstanceState.containsKey(EXTRA_SELECTED_PAYMENT_METHOD)) {
            mActivePaymentMethod = savedInstanceState.getInt(EXTRA_SELECTED_PAYMENT_METHOD);
        } else {
            mActivePaymentMethod = 0;
        }
        setupPaymentMethod();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_SELECTED_PAYMENT_METHOD, mActivePaymentMethod);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mPaymentMethodView.getId()) {
            if (mBraintreeFragment.getCachedPaymentMethods().size() > 1) {
                showPaymentMethodListDialog();
            }
        } else if (v.getId() == mChangeMethodView.getId()) {
            if (mBraintreeFragment.getCachedPaymentMethods().size() == 1) {
                launchFormView();
            } else {
                showPaymentMethodListDialog();
            }
        } else if (v.getId() == mSubmitButton.getId()) {
            mSubmitButton.setEnabled(false);
            mActivity.finalizeSelection(getActivePaymentMethod());
        }
    }

    protected void setupPaymentMethod() {
        mPaymentMethodView.setPaymentMethodDetails(getActivePaymentMethod());

        TextView link = findView(com.braintreepayments.api.dropin.R.id.bt_change_payment_method_link);
        if(mBraintreeFragment.getCachedPaymentMethods().size() == 1) {
            link.setText(com.braintreepayments.api.dropin.R.string.bt_add_payment_method);
        } else {
            link.setText(com.braintreepayments.api.dropin.R.string.bt_change_payment_method);
        }
    }

    private void showPaymentMethodListDialog() {
        PaymentMethodListAdapter paymentMethodListAdapter =
                new PaymentMethodListAdapter(mActivity, this, mBraintreeFragment.getCachedPaymentMethods());

        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(mActivity,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar);

        new AlertDialog.Builder(contextThemeWrapper)
            .setTitle(com.braintreepayments.api.dropin.R.string.bt_choose_payment_method)
            .setAdapter(paymentMethodListAdapter, paymentMethodListAdapter)
            .setPositiveButton(com.braintreepayments.api.dropin.R.string.bt_add_new_payment_method, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    launchFormView();
                }
            })
            .show();
    }

    private void launchFormView() {
        mActivity.showAddPaymentMethodView();
    }

    @Override
    public void onPaymentMethodSelected(int index) {
        mActivePaymentMethod = index;
        setupPaymentMethod();
    }

    private PaymentMethod getActivePaymentMethod() {
        return mBraintreeFragment.getCachedPaymentMethods().get(mActivePaymentMethod);
    }
}
