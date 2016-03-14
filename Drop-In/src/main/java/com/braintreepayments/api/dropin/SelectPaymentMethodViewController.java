package com.braintreepayments.api.dropin;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
    private Button mSubmitButton;

    private int mActivePaymentMethod;

    public SelectPaymentMethodViewController(BraintreePaymentActivity activity,
            Bundle savedInstanceState, View root, Braintree braintree, Customization customization) {
        super(activity, root, braintree, customization);
        mPaymentMethodView = findView(R.id.bt_selected_payment_method_view);
        mPaymentMethodView.setOnClickListener(this);

        mChangeMethodView = findView(R.id.bt_change_payment_method_link);
        mChangeMethodView.setOnClickListener(this);

        mSubmitButton = findView(R.id.bt_select_payment_method_submit_button);
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
            if (mBraintree.getCachedPaymentMethods().size() > 1) {
                showPaymentMethodListDialog();
            }
        } else if (v.getId() == mChangeMethodView.getId()) {
            if (mBraintree.getCachedPaymentMethods().size() == 1) {
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

        TextView link = findView(R.id.bt_change_payment_method_link);
        if(mBraintree.getCachedPaymentMethods().size() == 1) {
            link.setText(R.string.bt_add_payment_method);
        } else {
            link.setText(R.string.bt_change_payment_method);
        }
    }

    @SuppressWarnings("NewApi")
    private void showPaymentMethodListDialog() {
        PaymentMethodListAdapter paymentMethodListAdapter =
                new PaymentMethodListAdapter(mActivity, this, mBraintree.getCachedPaymentMethods());

        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(mActivity,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar);

        new AlertDialog.Builder(contextThemeWrapper)
            .setTitle(R.string.bt_choose_payment_method)
            .setAdapter(paymentMethodListAdapter, paymentMethodListAdapter)
            .setPositiveButton(R.string.bt_add_new_payment_method, new DialogInterface.OnClickListener() {
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

    protected void onPaymentMethodSelected(int paymentMethodIndex) {
        mActivePaymentMethod = paymentMethodIndex;
        setupPaymentMethod();
    }

    private PaymentMethod getActivePaymentMethod() {
        return mBraintree.getCachedPaymentMethods().get(mActivePaymentMethod);
    }
}
