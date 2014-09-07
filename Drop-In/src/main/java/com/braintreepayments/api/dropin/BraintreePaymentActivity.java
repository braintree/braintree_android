package com.braintreepayments.api.dropin;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.ErrorListener;
import com.braintreepayments.api.Braintree.PaymentMethodCreatedListener;
import com.braintreepayments.api.Braintree.PaymentMethodsUpdatedListener;
import com.braintreepayments.api.dropin.Customization.CustomizationBuilder;
import com.braintreepayments.api.dropin.view.PaymentButton;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.PayPalAccount;
import com.braintreepayments.api.models.PaymentMethod;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link android.app.Activity} encompassing Braintree's Drop-In UI.
 */
public class BraintreePaymentActivity extends Activity implements
        PaymentMethodsUpdatedListener, PaymentMethodCreatedListener, ErrorListener {

    protected static final String INTEGRATION_METHOD = "Drop-In";
    protected static final String ANALYTICS_PREFIX = "dropin.android";

    /**
     * The payment method flow halted due to a resolvable error (authentication, authorization, SDK upgrade required).
     * The reason for the error will be returned in a future release.
     */
    public static final int BRAINTREE_RESULT_DEVELOPER_ERROR = 2;

    /**
     * The payment method flow halted due to an error from the Braintree gateway.
     * The best recovery path is to try again with a new client token.
     */
    public static final int BRAINTREE_RESULT_SERVER_ERROR = 3;

    /**
     * The payment method flow halted due to the Braintree gateway going down for maintenance.
     * Try again later.
     */
    public static final int BRAINTREE_RESULT_SERVER_UNAVAILABLE = 4;

    /**
     * Used to specify a client token during initialization.
     */
    public static final String EXTRA_CLIENT_TOKEN = "com.braintreepayments.api.dropin.EXTRA_CLIENT_TOKEN";

    /**
     * Used to specify UI customizations during initialization.
     */
    public static final String EXTRA_CUSTOMIZATION = "com.braintreepayments.api.dropin.EXTRA_CUSTOMIZATION";

    /**
     * {@link com.braintreepayments.api.models.PaymentMethod} returned by successfully exiting the flow.
     */
    public static final String EXTRA_PAYMENT_METHOD = "com.braintreepayments.api.dropin.EXTRA_PAYMENT_METHOD";

    /**
     * Nonce returned by successfully exiting the flow. Can be used directly with the Braintree gateway.
     */
    public static final String EXTRA_PAYMENT_METHOD_NONCE = "com.braintreepayments.api.dropin.EXTRA_PAYMENT_METHOD_NONCE";

    private Braintree mBraintree;
    private AddPaymentMethodViewController mAddPaymentMethodViewController;
    private SelectPaymentMethodViewController mSelectPaymentMethodViewController;
    private AtomicBoolean mHasDataBeenReceived = new AtomicBoolean(false);
    private boolean mUnableToGetPaymentMethods = false;
    private PaymentMethod mActivePaymentMethod;
    private Bundle mSavedInstanceState;
    private Customization mCustomization;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSavedInstanceState = (savedInstanceState != null) ? savedInstanceState : new Bundle();

        setContentView(R.layout.dropin_ui_host);

        mCustomization = getCustomization();
        customizeActionBar();

        mBraintree = Braintree.getInstance(this, getClientToken());
        mBraintree.setIntegrationDropin();
        mBraintree.sendAnalyticsEvent("sdk.initialized");

        if (mBraintree.hasCachedCards()) {
            onPaymentMethodsUpdated(mBraintree.getCachedPaymentMethods());
        } else {
            mBraintree.getPaymentMethods();
            waitForData();
        }
    }

    protected void onResume() {
        super.onResume();

        mBraintree.addListener(this);
        mBraintree.unlockListeners();
    }

    protected void onPause() {
        super.onPause();

        mBraintree.lockListeners();
        mBraintree.removeListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == PaymentButton.REQUEST_CODE) {
                StubbedView.LOADING_VIEW.show(this);
                mAddPaymentMethodViewController.onPaymentResult(requestCode, resultCode, data);
            }
        } else if (resultCode == RESULT_CANCELED) {
            mBraintree.sendAnalyticsEvent(ANALYTICS_PREFIX + ".add-paypal.user-canceled",
                    INTEGRATION_METHOD);
        }
    }

    protected PaymentMethod getActivePaymentMethod() {
        return mActivePaymentMethod;
    }

    protected void setActivePaymentMethod(PaymentMethod paymentMethod) {
        mActivePaymentMethod = paymentMethod;
    }

    @Override
    public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
        if (!mUnableToGetPaymentMethods) {
            mHasDataBeenReceived.set(true);

            if (!paymentMethods.isEmpty()) {
                mActivePaymentMethod = paymentMethods.get(0);
            }
            showAppropriateView(paymentMethods);
        }
    }

    @Override
    public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
        mActivePaymentMethod = paymentMethod;

        if (paymentMethod instanceof Card) {
            mBraintree.sendAnalyticsEvent(ANALYTICS_PREFIX + ".add-card.success",
                    INTEGRATION_METHOD);
            mAddPaymentMethodViewController.showSuccess();

            Executors.newScheduledThreadPool(1).schedule(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finalizeSelection();
                        }
                    });
                }
            }, 1, TimeUnit.SECONDS);
        } else if (paymentMethod instanceof PayPalAccount) {
            mBraintree.sendAnalyticsEvent(ANALYTICS_PREFIX + ".add-paypal.success",
                    INTEGRATION_METHOD);
            mAddPaymentMethodViewController.endSubmit();
            initSelectPaymentMethodView();
        }
    }

    @Override
    public void onUnrecoverableError(Throwable throwable) {
        // Falling back to add payment method if getPaymentMethods fails
        if (StubbedView.LOADING_VIEW.mCurrentView) {
            mHasDataBeenReceived.set(true);
            initAddPaymentMethodView();
        } else {
            if(throwable instanceof AuthenticationException ||
                    throwable instanceof AuthorizationException ||
                    throwable instanceof UpgradeRequiredException ||
                    throwable instanceof ConfigurationException) {
                mBraintree.sendAnalyticsEvent(ANALYTICS_PREFIX + ".sdk.exit.developer-error",
                        INTEGRATION_METHOD);
                setResult(BRAINTREE_RESULT_DEVELOPER_ERROR);
            } else if(throwable instanceof ServerException || throwable instanceof UnexpectedException) {
                mBraintree.sendAnalyticsEvent(ANALYTICS_PREFIX + ".sdk.exit.server-error",
                        INTEGRATION_METHOD);
                setResult(BRAINTREE_RESULT_SERVER_ERROR);
            } else if(throwable instanceof DownForMaintenanceException) {
                mBraintree.sendAnalyticsEvent(ANALYTICS_PREFIX + ".sdk.exit.server-unavailable",
                        INTEGRATION_METHOD);
                setResult(BRAINTREE_RESULT_SERVER_UNAVAILABLE);
            }

            finish();
        }
    }

    @Override
    public void onRecoverableError(ErrorWithResponse error) {
        mAddPaymentMethodViewController.setErrors(error);
    }

    protected void finalizeSelection() {
        mBraintree.sendAnalyticsEvent(ANALYTICS_PREFIX + ".sdk.exit.success", INTEGRATION_METHOD);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_PAYMENT_METHOD, mActivePaymentMethod);
        resultIntent.putExtra(EXTRA_PAYMENT_METHOD_NONCE, mActivePaymentMethod.getNonce());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void waitForData() {
        Executors.newScheduledThreadPool(1).schedule(new Runnable() {
            @Override
            public void run() {
                if (!mHasDataBeenReceived.get()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mUnableToGetPaymentMethods = true;
                            initAddPaymentMethodView();
                        }
                    });
                }
            }
        }, 10, TimeUnit.SECONDS);
        StubbedView.LOADING_VIEW.show(this);
    }

    private void showAppropriateView(List<PaymentMethod> paymentMethods) {
        if (paymentMethods.size() == 0) {
            initAddPaymentMethodView();
        } else {
            initSelectPaymentMethodView();
        }
    }

    private void initSelectPaymentMethodView() {
        View selectMethodView = StubbedView.SELECT_VIEW.show(this);

        if (mSelectPaymentMethodViewController == null) {
            mSelectPaymentMethodViewController = new SelectPaymentMethodViewController(this,
                    mSavedInstanceState, selectMethodView, mBraintree, mCustomization);
        }

        setActionBarUpEnabled(false);
    }

    protected void initAddPaymentMethodView() {
        mBraintree.sendAnalyticsEvent(ANALYTICS_PREFIX + ".add-card.start", INTEGRATION_METHOD);

        View paymentMethodView = StubbedView.CARD_FORM.show(this);

        if (mAddPaymentMethodViewController == null) {
            mAddPaymentMethodViewController = new AddPaymentMethodViewController(this,
                    mSavedInstanceState, paymentMethodView, mBraintree, mCustomization);
        }

        if (mBraintree.getCachedPaymentMethods().size() > 0) {
            setActionBarUpEnabled(true);
        }
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    private void setActionBarUpEnabled(boolean enabled) {
        if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(enabled);
            }
        }
    }

    private String getClientToken() {
        String clientToken = getIntent().getStringExtra(EXTRA_CLIENT_TOKEN);
        if (clientToken == null) {
            clientToken = mSavedInstanceState.getString(EXTRA_CLIENT_TOKEN);
            if (clientToken == null) {
                throw new IllegalArgumentException("A client token must be specified with " +
                        BraintreePaymentActivity.class.getSimpleName() +
                        ".EXTRA_CLIENT_TOKEN extra");
            }
        }
        return clientToken;
    }

    @SuppressLint("NewApi")
    private void customizeActionBar() {
        if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                if (TextUtils.isEmpty(mCustomization.getActionBarTitle())) {
                    actionBar.setTitle(getString(R.string.default_action_bar_text));
                } else {
                    actionBar.setTitle(mCustomization.getActionBarTitle());
                }

                if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {
                    if (mCustomization.getActionBarLogo() == 0) {
                        actionBar.setLogo(new ColorDrawable(
                                getResources().getColor(android.R.color.transparent)));
                    } else {
                        actionBar.setLogo(mCustomization.getActionBarLogo());
                    }
                }
            }
        }
    }

    private Customization getCustomization() {
        Customization customization = (Customization) getIntent().getSerializableExtra(EXTRA_CUSTOMIZATION);
        if (customization == null) {
            customization = new CustomizationBuilder()
                    .build();
        }
        return customization;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (StubbedView.CARD_FORM.mCurrentView && mBraintree.getCachedPaymentMethods().size() > 0) {
            initSelectPaymentMethodView();
        } else if (mAddPaymentMethodViewController != null &&
                mAddPaymentMethodViewController.isSubmitting()) {
            // noop
        } else {
            mBraintree.sendAnalyticsEvent(ANALYTICS_PREFIX + ".sdk.exit.user-canceled",
                    INTEGRATION_METHOD);
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_CLIENT_TOKEN, getClientToken());
        saveState(mAddPaymentMethodViewController, outState);
        saveState(mSelectPaymentMethodViewController, outState);
    }

    private void saveState(BraintreeViewController viewController, Bundle outState) {
        if (viewController != null) {
            viewController.onSaveInstanceState(outState);
        }
    }

    /**
     * A simple interface to decide which of the central views should be displayed. The description
     * is not included here since it is included with all views if it is specified.
     */
    private enum StubbedView {
        LOADING_VIEW(R.id.stub_loading_view, R.id.inflated_loading_view),
        SELECT_VIEW(R.id.stub_payment_methods_list, R.id.inflated_payment_methods_list),
        CARD_FORM(R.id.stub_payment_method_form, R.id.inflated_payment_method_form);

        private final int mStubbedViewId;
        private final int mInflatedViewId;
        private boolean mCurrentView;

        private static int mAnimationDuration;

        StubbedView(int stubbedViewId, int inflatedViewId) {
            mStubbedViewId = stubbedViewId;
            mInflatedViewId = inflatedViewId;
            mCurrentView = false;
        }

        /**
         * @param activity hosting activity for the views. Should always be {@code this}.
         * @return the displayed {@link View}.
         */
        @SuppressLint("NewApi")
        @SuppressWarnings("unchecked")
        <T extends View> T show(BraintreePaymentActivity activity) {
            for (StubbedView value : values()) {
                if (this != value) {
                    value.hide(activity);
                }
            }

            ViewStub stub = activity.findView(mStubbedViewId);
            View inflated;
            if (stub != null) {
                inflated = stub.inflate();
            } else {
                inflated = activity.findView(mInflatedViewId);
            }

            if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1) {
                inflated.setAlpha(0f);
                inflated.setVisibility(View.VISIBLE);
                inflated.animate()
                        .alpha(1f)
                        .setDuration(getDuration(activity));
            } else {
                inflated.setVisibility(View.VISIBLE);
            }

            mCurrentView = true;

            return (T) inflated;
        }

        /**
         * @param activity hosting activity for the views. Should always be {@code this}.
         */
        void hide(BraintreePaymentActivity activity) {
            ViewStub stub = activity.findView(mStubbedViewId);
            if (stub == null) {
                activity.findView(mInflatedViewId).setVisibility(View.GONE);
            }

            mCurrentView = false;
        }

        private long getDuration(Context context) {
            if (mAnimationDuration == 0) {
                mAnimationDuration = context.getResources().getInteger(
                        android.R.integer.config_shortAnimTime);
            }

            return mAnimationDuration;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T findView(int id) {
        return (T) findViewById(id);
    }

}
