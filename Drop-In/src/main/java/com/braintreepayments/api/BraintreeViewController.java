package com.braintreepayments.api;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.braintreepayments.api.dropin.Customization;

/**
 * Parent class for view controllers.
 */
public abstract class BraintreeViewController {

    private View mRootView;
    protected BraintreePaymentActivity mActivity;
    protected final BraintreeFragment mBraintreeFragment;
    private final Customization mCustomization;

    public BraintreeViewController(BraintreePaymentActivity activity, View root,
            BraintreeFragment braintreeFragment, Customization customization) {
        mActivity = activity;
        mRootView = root;
        mBraintreeFragment = braintreeFragment;
        mCustomization = customization;
        initDescriptionView();
    }

    /**
     * @see android.app.Activity#onSaveInstanceState(Bundle)
     */
    public abstract void onSaveInstanceState(Bundle outState);

    protected String getSubmitButtonText() {
        String submitText = getCustomizedCallToAction();

        if(!TextUtils.isEmpty(mCustomization.getAmount())) {
            submitText = mCustomization.getAmount() + " - " + submitText;
        }

        return submitText.toUpperCase();
    }

    protected String getCustomizedCallToAction() {
        String actionText = mCustomization.getSubmitButtonText();

        if (TextUtils.isEmpty(actionText)) {
            actionText = mActivity.getString(com.braintreepayments.api.dropin.R.string.bt_default_submit_button_text);
        }

        return actionText;
    }

    private void initDescriptionView() {
        if (!TextUtils.isEmpty(mCustomization.getPrimaryDescription())) {
            initDescriptionSubview(com.braintreepayments.api.dropin.R.id.bt_primary_description, mCustomization.getPrimaryDescription());
            initDescriptionSubview(com.braintreepayments.api.dropin.R.id.bt_secondary_description, mCustomization.getSecondaryDescription());
            initDescriptionSubview(com.braintreepayments.api.dropin.R.id.bt_description_amount, mCustomization.getAmount());

            findView(com.braintreepayments.api.dropin.R.id.bt_description_container).setVisibility(View.VISIBLE);
        }
    }

    private void initDescriptionSubview(int id, String text) {
        if (!TextUtils.isEmpty(text)) {
            TextView subview = findView(id);
            subview.setText(text);
        }
    }

    /**
     * Concentrate casting View objects into one place for flexibility and convenience.
     */
    @SuppressWarnings("unchecked")
    public <T extends View> T findView(int id) {
        return (T) mRootView.findViewById(id);
    }

}
