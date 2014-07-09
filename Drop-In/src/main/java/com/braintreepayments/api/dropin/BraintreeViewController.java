package com.braintreepayments.api.dropin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.braintreepayments.api.Braintree;

import java.lang.ref.WeakReference;

/**
 * Parent class for view controllers.
 */
public abstract class BraintreeViewController {

    private View mRootView;
    /**
     * Weak reference to the containing activity so as to not cause a memory leak.
     */
    private final WeakReference<BraintreePaymentActivity> mActivity;
    private final Braintree mBraintree;
    private final Customization mCustomization;

    public BraintreeViewController(BraintreePaymentActivity activity, View root, Braintree braintree, Customization customization) {
        mActivity = new WeakReference<BraintreePaymentActivity>(activity);
        mRootView = root;
        mBraintree = braintree;
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
            submitText += " - " + mCustomization.getAmount();
        }

        return submitText;
    }

    protected String getCustomizedCallToAction() {
        String actionText = mCustomization.getSubmitButtonText();

        if (TextUtils.isEmpty(actionText)) {
            actionText = getActivity().getString(R.string.default_submit_button_text);
        }

        return actionText;
    }

    private void initDescriptionView() {
        if (!TextUtils.isEmpty(mCustomization.getPrimaryDescription())) {
            initDescriptionSubview(R.id.primary_description, mCustomization.getPrimaryDescription());
            initDescriptionSubview(R.id.secondary_description, mCustomization.getSecondaryDescription());
            initDescriptionSubview(R.id.description_amount, mCustomization.getAmount());

            findView(R.id.description_container).setVisibility(View.VISIBLE);
        }
    }

    private void initDescriptionSubview(int id, String text) {
        if (!TextUtils.isEmpty(text)) {
            TextView subview = findView(id);
            subview.setText(text);
        }
    }

    protected Braintree getBraintree() {
        return mBraintree;
    }

    protected BraintreePaymentActivity getActivity() {
        return mActivity.get();
    }

    /**
     * Concentrate casting View objects into one place for flexibility and convenience.
     */
    @SuppressWarnings("unchecked")
    public <T extends View> T findView(int id) {
        return (T) mRootView.findViewById(id);
    }

}
